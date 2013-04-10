package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer


abstract class TCPProtocol{

  var connection: TCPConnection

  //interface for server
  def build(conn: TCPConnection): TCPProtocol
  //def connectionMade(conn: TCPConnection) 
  //def dataReceived(conn: TCPConnection, data: Array[Byte])
  //def bytesToWrite(conn: TCPConnection): Array[Bytes]

  def needs_write: Boolean

  //server tells us that a socket we previously requested to be monitored for
  //WRITEability is now ready
  def write(): Unit = connection.write()

  //server tells us a socket is readable, read some data and notify
  //the handlers if we have a complete protocol message
  def read(handlers: Seq[Function2[TCPProtocol, Array[Byte],Unit]])

  //interface for clients to interact with protocol
  
  //add a handler function which will fire on protocol messages
  def onRead(handler: Function2[TCPConnection,Array[Byte],Unit]) = ()

  //add a handler function which will fire on new connections
  def onConnect(handler: Function1[TCPConnection,Unit]) = ()

  //schedule the data to be written
  def write(bytes: Array[Byte]) = ()
}


/**
  * a thin wrapper class which passes all reads and writes directly to the underlying TCP transport without further processing
  */
class BasicTCP() extends TCPProtocol{
  private val readHandlers: ArrayBuffer[Function2[TCPConnection,Array[Byte],Unit]] = ArrayBuffer()
  var connection: TCPConnection = null
  override def build(conn: TCPConnection): BasicTCP = {
    val res = new BasicTCP()
    res.connection = conn
    res
  }

  override def read(handlers: Seq[Function2[TCPProtocol, Array[Byte],Unit]]) = {
    connection.read match{
      case Some(data) => {
        handlers.foreach{_(this,data)}
        readHandlers.foreach{_(connection,data)}
      }
      case None => ()
    }
  }
  override def write(bytes: Array[Byte]) = connection.write(bytes)
  override def needs_write = connection.needs_write
  override def onRead(handler: Function2[TCPConnection,Array[Byte],Unit]) = {
    println("adding handlers")
    readHandlers += handler
  }
}



final class TCPServer(private val protocol: TCPProtocol){

  private var serverChannel: ServerSocketChannel = _
  private var selector: Selector = _

  private val readHandlers: ArrayBuffer[Function2[TCPProtocol,Array[Byte],Unit]] = ArrayBuffer()


  def onRead(handler: Function2[TCPProtocol,Array[Byte],Unit]) = readHandlers += handler

  def socket = serverChannel.socket

  /**
   * Start listening for TCP connections
   * @param address the address to bind to
   * @param blocking block on this method
   */
  def listen(address: InetSocketAddress, blocking: Boolean = true)={
    selector = Selector.open()

    serverChannel = ServerSocketChannel.open()
    serverChannel.socket().setReuseAddress(true) 
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(address)

    serverChannel.register(selector, SelectionKey.OP_ACCEPT)

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
    /*val all_iter = selector.keys().iterator()
    println(s"selector.keys().size(): ${selector.keys().size()}")
    while(all_iter.hasNext){
      val key = all_iter.next()
      debugKey(key)
    }*/

    val available = selector.select() 
    assert(available > 0) //select blocks until we have channel activity
                          //so this is just a sanity check

    val keyset = selector.selectedKeys()
    val iter = keyset.iterator()
    while(iter.hasNext){
      val key = iter.next()
      iter.remove() //remove key from selection group
      if ( key.isReadable){ doRead(key)}
      else if (key.isAcceptable){ doAccept(key, serverChannel) }
      else if (key.isWritable){ doWrite(key)}
    }
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
          key.cancel()
          return ()
        }
    }
    assert(channel != null)  //shouldn't happen because the selector 
                             //tells us that we had a pending connection
                             //and we handle IO exception above

    val transport = new TCPConnection(UUID.randomUUID, key, channel)
    val impl  = protocol.build(transport)

    //make it non-blocking
    channel.configureBlocking(false);

    //register this channel with the event loop's selector and 
    //attach protocol implementation to channel
    //all channels are monitored for READability 
    channel.register(selector, SelectionKey.OP_READ).attach(impl);
    //protocol.connectionMade(transport)
  }

  /**
  * Tell the transport layer to do a write
  * @param key the SelectionKey whose socket should be written
  */
  private def doWrite(key: SelectionKey){
      val proto = key.attachment.asInstanceOf[TCPProtocol]
      //val bytes = def bytesToWrite(): Array[Bytes]
      if(proto.needs_write)
      {
        //do the write
        proto.write()
      }
      else
      {
        println("no data to write, cancelling WRITE registration")
        val newops = key.interestOps() & ~SelectionKey.OP_WRITE 
        key.interestOps(newops)//nothing to write, cancel WRITE registration
      }
  }


  /**
  * Tell the transport layer to read
  * @param globalReadBuffer
  * @param key
  */
  private def doRead(key: SelectionKey){
      val proto = key.attachment.asInstanceOf[TCPProtocol]
      proto.read(readHandlers)
    }



  /**
  * Close the server socket channel and stop accepting new requests
  * @param forceClose if true, close all existing client connections and shutdown the selector
  */
  def shutdown(forceClose: Boolean = false)={
    serverChannel.socket().close()
    serverChannel.close()
    if(forceClose){ //close all active connections
      val iter = selector.keys().iterator()
      while(iter.hasNext){
        val key = iter.next()
        key.cancel()
        key.channel().close()
        iter.remove()
      }
      selector.close()
    }
  }
}

object net {
  /**
  * builds a new TCP server
 e* @return
  */
  def createServer(): TCPServer = new TCPServer(new BasicTCP())
}
