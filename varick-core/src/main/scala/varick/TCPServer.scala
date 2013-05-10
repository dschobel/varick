package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._


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
      println(s"\tVALID KEY  \treadable? ${key.isReadable()} " +
      s"writable? ${key.isWritable()}   acceptable?  ${key.isAcceptable}")
    }
    else{
      println("\tINVALID KEY")
    }
  }

  /**
  * Execute one round of the event loop 
  */
  private def tick():Unit = {
    /*val all_iter = SystemSelector.keys().iterator()
    println(s"SystemSelector.keys().size(): ${SystemSelector.keys().size()}")
    while(all_iter.hasNext){
      val key = all_iter.next()
      debugKey(key)
    }*/

    val available = SystemSelector.select() 
    assert(available > 0) //select blocks until we have channel activity
                          //so this is just a sanity check


    val writable: collection.mutable.Set[SelectionKey] = SystemSelector.selectedKeys().filter{_.isWritable}
    val iter = SystemSelector.selectedKeys().iterator()
    while(iter.hasNext){
      val key = iter.next()
      if ( key.isReadable){ doRead(key, writable)}
      else if (key.isAcceptable){ doAccept(key, serverChannel) }
      else if (key.isWritable){ doWrite(key)}

      iter.remove() //key has been handled, remove it from selection group
    }
  }

  /**
    * monitor an external SocketChannel in this server's event loop
    * @param channel the channel to monitor
    * @param interestOps the operations to monitor
    */
  def monitor(channel: SocketChannel, interestOps: Int = SelectionKey.OP_READ): TCPConnection = {
    if(channel.isBlocking){
      throw new Error("blocking socket channels are not allowed")
    }
    val key = Option(channel.keyFor(SystemSelector)) getOrElse channel.register(SystemSelector, interestOps)

    val connection = new TCPConnection(UUID.randomUUID, key, channel)
    val codecImpl = builder.build(connection)
    key.attach(codecImpl)
    connection
  }

  /**
  * Accept a new TCP connection
  * @param key the SelectionKey which triggered the accept action
  * @param server the server socket which will do the accepting
  */
  private def doAccept(key: SelectionKey, server: ServerSocketChannel){
    var channel: SocketChannel = null
    try{
      channel = server.accept()
    }catch{
      case ioe: java.io.IOException => { 
          println(s"ERROR: caught ${ioe.getMessage} when trying to ACCEPT")
          key cancel()
          key attach null   //detach TCPConnection object
          return ()
        }
    }
    assert(channel != null)  //shouldn't happen because the SystemSelector 
                             //tells us that we had a pending connection
                             //and we handle IO exception above


    val transport = new TCPConnection(UUID.randomUUID, key, channel)
    val impl = builder.build(transport)

    //make it non-blocking
    channel.configureBlocking(false)

    //register this channel with the event loop's SystemSelector and 
    //attach protocol implementation to channel
    //all channels are monitored for READability 
    channel.register(SystemSelector, SelectionKey.OP_READ,impl)
    connHandlers.foreach{_(transport)}
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
        println("no data to write, cancelling WRITE registration")
        val newops = key.interestOps() & ~SelectionKey.OP_WRITE 
        key.interestOps(newops)//nothing to write, cancel WRITE registration
      }
  }


  /**
  * Read some bytes from the TCP connection, delegate bytes to protocol
  * for encoding and processing and then trigger read callbacks
  * @param key the SelectionKey which is ready for a read
  */
  private def doRead(key: SelectionKey, writableKeys: collection.mutable.Set[SelectionKey]){

    val codec = key.attachment.asInstanceOf[T]
    val downstream_writable = codec.connection.downStream.forall{ writableKeys.contains(_) }
    if(! downstream_writable){
      //suppress read since downstream is not writabl
      println("downstream is not writable, suppressing read")
    }

    for {
      bytes <- codec.connection.read()
      result <- codec.process(bytes)
    } readHandlers.foreach{_(codec.connection,result)}
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
