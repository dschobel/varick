package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer


/**
 * a server for handling protocols built on top of TCP
 * @param builder the ProtocolBuilder which instantiates a new codec for en/decoding the underlying stream of bytes
 * @tparam T the tcp-based protocol to be en/decoded
 */
final class TCPServer[T <: TCPCodec[D],D](private val builder: ProtocolBuilder[T]){

  private var serverChannel: ServerSocketChannel = _
  private var selector: Selector = _

  private val readHandlers: ArrayBuffer[Function2[TCPCodec[D],D,Unit]] = ArrayBuffer()

  //add a new callback for read events of the underlying codec (ie; handle a new HTTP request)
  def onRead(handler: Function2[TCPCodec[_],D,Unit]) = readHandlers += handler

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
    val impl = builder.build(transport)

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
      val codec = key.attachment.asInstanceOf[TCPCodec[D]]
      //val bytes = def bytesToWrite(): Array[Bytes]
      if(codec.needs_write)
      {
        //do the write
        codec.write()
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
  * @param key
  */
private def doRead(key: SelectionKey){

      val codec = key.attachment.asInstanceOf[TCPCodec[D]]
      //T <: TCPCodec

      //readhandlers: ArrayBuffer[Function2[_ >: T,T#ProtocolData,Unit]] 

      //dispatch read handlers to codec so that it can call
      //them if it determines that a complete message has arrived
      val seqHandlers = readHandlers.toSeq
      codec.read(seqHandlers)
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
  * @return
  */
  def createServer(): TCPServer[BasicTCP,Array[Byte]] = new TCPServer(TCPBuilder)
}
