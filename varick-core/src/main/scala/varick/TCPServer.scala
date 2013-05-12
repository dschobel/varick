package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.util.UUID
import concurrent._
import concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer
import scala.util.{Try,Failure,Success}


/**
 * a server for handling protocols built on top of TCP
 * @param builder the ProtocolBuilder which instantiates a new codec for en/decoding the underlying stream of bytes
 * @tparam T the tcp-based protocol to be en/decoded
 */
final class TCPServer[T <: TCPCodec](private val builder: ProtocolBuilder[T]){

  private var serverChannel: ServerSocketChannel = _
  private var SystemSelector: Selector = _

  private val readHandlers = ArrayBuffer[Function2[TCPConnection,T#ProtocolData,Unit]]()
  private val connHandlers = ArrayBuffer[Function1[TCPConnection,Unit]]()

  //register a new callback for request events of the underlying codec (ie; handle a new HTTP request)
  def onRequest(handler: Function2[TCPConnection,T#ProtocolData,Unit]) = readHandlers += handler

  //register a new callback for a connection event
  def onConnection(handler: Function1[TCPConnection,Unit]) = connHandlers += handler

  def socket = serverChannel.socket

  /**
   * Start listening for TCP connections
   * @param address the address to bind to
   * @param blocking block on this method
   */
  def listen(address: InetSocketAddress, blocking: Boolean = true)={
    SystemSelector = Selector.open()

    serverChannel = ServerSocketChannel.open()
    serverChannel.socket().setReuseAddress(true) 
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(address)

    serverChannel.register(SystemSelector, SelectionKey.OP_ACCEPT)

    if (blocking){
      while(true){ tick() }
    }
    else{//don't block thread (for easier testing)
      Future{  while(true){tick()}  }
    }
  }

  /**
   * Print debug information
   * @param key the SelectionKey whose status is printed
   */
  private def debugKey(key: SelectionKey): Unit = {
    if(key.isValid()){

      val conn = key.attachment.asInstanceOf[T]
      val idstr = Option(conn)  match{
        case Some(conn) => conn.connection.id.toString
        case _ => "SERVER"
      }
      println(s"\t$idstr:  \treadable? ${key.isReadable()} writable? ${key.isWritable()}  acceptable?  ${key.isAcceptable}")
    }
    else{
      println("\tINVALID KEY")
    }
  }

  private def debugSelector(selector: Selector): Unit ={
    val all_iter = selector.keys().iterator()
    println(s"selector.keys().size(): ${selector.keys().size()}")
    while(all_iter.hasNext){
      val key = all_iter.next()
      debugKey(key)
    }
  }

  /**
   * Execute one round of the event loop 
   */
  private def tick(): Unit = {

    val available = SystemSelector.select() 

    assert(available > 0) //select blocks until we have channel activity
                          //so this is just a sanity check

     
    val iter = SystemSelector.selectedKeys().iterator()
    while(iter.hasNext){
      val key = iter.next()
      if (key.isValid && key.isReadable){ doRead(key) }
      else if (key.isValid && key.isAcceptable){ doAccept(key, serverChannel) }
      else if (key.isValid && key.isWritable){ doWrite(key) }

      iter.remove() //key has been handled, remove it from selection group
    }
  }

  /**
   * Monitor an external SocketChannel in this server's event loop
   * @param channel the channel to monitor
   * @param interestOps the operations to monitor besides WRITE
   * @note channels are necessarily monitored for WRITE readiness as
   * this is fundamental to our congestion back pressure algorithm. Any 
   * interest ops passed in are in addition to WRITE.
   */
  def monitor(channel: SocketChannel, interestOps: Int = 0): TCPConnection = {
    if(channel.isBlocking){
      throw new Error("blocking socket channels are not allowed")
    }
    val key = channel.register(SystemSelector, interestOps | SelectionKey.OP_WRITE)
    val codec = Option(key attachment)
    codec match{
      case None => {
          val connection = new TCPConnection(UUID.randomUUID, key, channel)
          println(s"key had no attachment, creating new connection with id ${connection.id}")
          val codecImpl = builder.build(connection)
          key.attach(codecImpl)
          connection
      }
      case Some(attachment) => {
        val codec = attachment.asInstanceOf[T]
        println("key already has attachment, reusing TCPConnection ${codec.connection.id}")
        codec.connection
      }
    }
  }

  /**
   * Accept a new TCP connection
   * @param key the SelectionKey which triggered the accept action
   * @param server the server socket which will do the accepting
   */
  private def doAccept(key: SelectionKey, server: ServerSocketChannel){
    Try(server.accept()) match{
      case Failure(e) => {
          println(s"ERROR: caught ${e.getMessage} when trying to ACCEPT")
          key cancel()
          key attach null   //detach TCPConnection object
      }
      case Success(channel) =>  {
          val transport = new TCPConnection(UUID.randomUUID, key, channel)
          val impl = builder.build(transport)

          //make it non-blocking
          channel.configureBlocking(false)

          //register this channel with the event loop's Selector and 
          //attach protocol implementation to channel
          //all channels are monitored for READability 
          channel.register(SystemSelector, SelectionKey.OP_READ,impl)
          connHandlers.foreach{_(transport)}
      }
    }
  }

  /**
   * Tell the transport layer to do a write
   * @param key the SelectionKey whose socket should be written
   */
  private def doWrite(key: SelectionKey){
      val codec = key.attachment.asInstanceOf[T]
      if(codec.needs_write)
      {
        //do the write
        codec.connection.writeBuffered()
      }
      else
      {
          val upstream = codec.connection.upStream.size > 0
          //if(upstream){ println(s"${codec.connection.id} has an upstream connection") }
          println(s"${codec.connection.id} has no data to write, cancelling WRITE registration")
          val newops = key.interestOps() & ~SelectionKey.OP_WRITE 
          key.interestOps(newops)//nothing to write, cancel WRITE registration
      }
  }

  private def keyAsCodec(key: SelectionKey) = Option(key.attachment.asInstanceOf[T])

  private def keyId(key: SelectionKey): Option[UUID] = keyAsCodec(key).map{_.connection.id}

  /**
   * Read some bytes from the TCP connection, delegate bytes to protocol
   * for encoding and processing and then trigger read callbacks
   * @param key the SelectionKey which is ready for a read
   */
  private def doRead(key: SelectionKey){

    val codec = key.attachment.asInstanceOf[T]
    //Console println ("downstream keys: " + codec.connection.downStream.map(keyAsCodec).collect{ case Some(codec) => codec.connection.id + ": isWritable? " + codec.connection.key.isWritable}.mkString(","))
    if(! codec.connection.downStream.forall{ _.isWritable })
    {
      //suppress read since downstream is not writable
      println("downstream is not writable, suppressing read")
    }
    else{
      for {
        bytes <- codec.connection.read()
        result <- codec.process(bytes)
      } readHandlers.foreach{_(codec.connection,result)}
    }
  }


  /**
   * Close the server socket channel and stop accepting new requests
   * @param closeExistingConnections if true, close all existing client connections and shutdown the SystemSelector
   */
  def shutdown(closeExistingConnections: Boolean = false)={
    serverChannel.close() //stop accepting new connections
    if(closeExistingConnections){ //close all active connections
      val iter = SystemSelector.keys().iterator()
      while(iter.hasNext){
        val key = iter.next()
        try{
          key.channel().close()
        }catch{
          case exn: Exception => println("caught: " +  exn.getMessage() + " while trying to close a client channel")
        }
      }
      SystemSelector.close()
    }
  }
}

object net {
  /**
   * builds a new TCP server
   * @return
   */
  def createServer() = new TCPServer(TCPBuilder)
}
