package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global


trait TCPProtocol{
  def dataReceived(stream: Stream, data: Array[Byte])
  //def connectionLost() = ()
  //def makeConnection() = ()
  def onRead(handler: Function2[Stream,Array[Byte],Unit])
  def connectionMade(stream: Stream) 
}


object BasicTCP extends TCPProtocol{
  import collection.mutable.ArrayBuffer

  private val readHandlers: ArrayBuffer[Function2[Stream,Array[Byte],Unit]] = ArrayBuffer()
  private val acceptHandlers: ArrayBuffer[Function1[Stream,Unit]] = ArrayBuffer()
  override def connectionMade(stream: Stream) = acceptHandlers.foreach{_(stream)}
  override def dataReceived(stream: Stream, data: Array[Byte]) = readHandlers.foreach{_(stream, data)}

  override def onRead(handler: Function2[Stream,Array[Byte],Unit]) = readHandlers += handler
  def onAccept(handler: Function1[Stream,Unit]) = acceptHandlers += handler
}



final class Server(val protocol: TCPProtocol, globalReadBufferSz: Int = 1024){

  private var serverChannel: ServerSocketChannel = _
  private var selector: Selector = _


  def onRead(handler: Function2[Stream,Array[Byte],Unit]) = protocol.onRead(handler)
  def connectionMade(stream: Stream) = protocol.connectionMade(stream)
  def dataReceived(stream: Stream, data: Array[Byte]) = protocol.dataReceived(stream, data)

  def socket = serverChannel.socket

  /**
   * Start listening for TCP connections
   * @param address the address to bind to
   * @param blocking block on this method
   */
  def listen(address: InetSocketAddress, blocking: Boolean = true)={
    selector = Selector.open()
    val globalReadBuffer = ByteBuffer.allocate(globalReadBufferSz)

    serverChannel = ServerSocketChannel.open()
    serverChannel.socket().setReuseAddress(true) 
    //println(s"receive buffer size is ${serverChannel.socket().getReceiveBufferSize}")
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(address)

    serverChannel.register(selector, SelectionKey.OP_ACCEPT)

    if (blocking){
      while(true){ tick(globalReadBuffer) }
    }
    else{//don't block thread (for easier testing)
      Future{  while(true){tick(globalReadBuffer)}  }
    }
  }

  /**
  * Print debug information
  * @param key the SelectionKey whose status is printed
  */
  private def debugKey(key: SelectionKey): Unit = {
    println(s"\treadable? ${key.isReadable()}  " +
      s"writable? ${key.isWritable()}   acceptable?  ${key.isAcceptable}")
  }

  /**
  * Execute one round of the event loop 
  * @param globalReadBuffer
  */
  private def tick(globalReadBuffer: ByteBuffer):Unit = {
    //println(s"selector.keys().size(): ${selector.keys().size()}")
    val available = selector.select() 
    //println(s"$available keys are ready for operations")

    assert(available > 0) //select blocks until we have channel activity
                          //so this is just a sanity check

    val keyset = selector.selectedKeys()
    val iter = keyset.iterator()
    while(iter.hasNext){
      val key = iter.next()
      iter.remove() //remove key from selection group
      if ( key.isReadable){ doRead(globalReadBuffer, key)}
      else if (key.isAcceptable){ 
        //val all_iter = selector.keys().iterator()
        //println(s"selector.keys().size(): ${selector.keys().size()}")
        //while(all_iter.hasNext){
          //val key = all_iter.next()
          //debugKey(key)
        //}

        doAccept(key, serverChannel) 
      }
      else if (key.isWritable){ doWrite(key)}
    }
  }

  /**
  * Write buffered data to key's underlying socket
  * @param key the SelectionKey whose socket should be written
  */
  private def doWrite(key: SelectionKey){
      val stream = key.attachment.asInstanceOf[Stream]
      val channel = key.channel().asInstanceOf[SocketChannel]
      if(stream.needs_write)
      {
        //do the write
        stream.write()
      }
      else
      {
        println("no data to write, cancelling WRITE registration")
        val newops = key.interestOps() & ~SelectionKey.OP_WRITE 
        key.interestOps(newops)//nothing to write, cancel WRITE registration
      }
  }

  /**
  *
  * @param globalReadBuffer
  * @param key
  */
  private def doRead(globalReadBuffer: ByteBuffer, key: SelectionKey){
      val stream = key.attachment.asInstanceOf[Stream]
      val channel = key.channel().asInstanceOf[SocketChannel]
      globalReadBuffer.clear()
      var bytesRead = 0
      try{
        bytesRead = channel.read(globalReadBuffer)
      } catch{ 
        case ioe: java.io.IOException => { stream.close(); key.cancel(); }
      }
      if (bytesRead != -1) {
        globalReadBuffer.flip()
        val data = globalReadBuffer.array.take(bytesRead)
        protocol.dataReceived(stream,data)
        //println(s"server got: ${new String(data)}")
      }
      else{ stream.close(); key.cancel() }
    }


  /**
  *
  * @param key
  * @param server
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
    assert(channel != null)//shouldn't happen because the selector 
                           //tells us that we had a pending connection
                           //and we handle IO exception above

    val stream = new Stream(UUID.randomUUID,key,channel.asInstanceOf[SocketChannel])
    //println(s"accepted new channel, assigning id ${stream.id.toString}")
    //make it non-blocking as well
    channel.configureBlocking(false);

    //register this channel with the event loop's selector and attach UUID to channel
    //all channels are monitored for READability 
    channel.register(selector, SelectionKey.OP_READ).attach(stream);
    protocol.connectionMade(stream)
  }

  /**
  *
  * @param forceClose
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
  *
  * @return
  */
  def createServer(): Server = new Server(BasicTCP)
}
