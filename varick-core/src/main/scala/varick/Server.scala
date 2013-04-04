package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.{ArrayBuffer,Set}

final class Server(globalReadBufferSz: Int = 1024){

  private var serverChannel: ServerSocketChannel = _
  private var selector: Selector = _
  private var acceptHandlers: ArrayBuffer[Function1[Stream,Unit]] = ArrayBuffer()
  private val allStreams: Set[Stream] = Set()

  def socket = serverChannel.socket

  def onAccept(handler: Function1[Stream,Unit]) = acceptHandlers += handler

  def listen(address: InetSocketAddress, blocking: Boolean = true)={
    selector = Selector.open()
    val globalReadBuffer = ByteBuffer.allocate(globalReadBufferSz)

    serverChannel = ServerSocketChannel.open()
    serverChannel.socket().setReuseAddress(true) 
    //serverChannel.socket().setReceiveBufferSize(2000)
    println(s"receive buffer size is ${serverChannel.socket().getReceiveBufferSize}")
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

  private def debugKey(key: SelectionKey): Unit = {
    println(s"\treadable? ${key.isReadable()}  " +
      s"writable? ${key.isWritable()}   acceptable?  ${key.isAcceptable}")
  }

  private def tick(globalReadBuffer: ByteBuffer):Unit = {
    //println(s"selector.keys().size(): ${selector.keys().size()}")
    val available = selector.select() 
    /*val all_iter = selector.keys().iterator()
    while(all_iter.hasNext){
      val key = all_iter.next()
      debugKey(key)
    }
    */
    //println(s"$available keys are ready for operations")

    assert(available > 0) //select blocks until we have channel activity
                          //so this is just a sanity check

    val keyset = selector.selectedKeys()
    val iter = keyset.iterator()
    while(iter.hasNext){
      val key = iter.next()
      //debugKey(key)
      iter.remove() //remove key from selection group
      if (key.isReadable){ doRead(globalReadBuffer, key)}
      else if (key.isAcceptable){ doAccept(key, serverChannel) }
      else if (key.isWritable){ doWrite(key)}
    }
  }

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
        //println(s"server got: ${new String(data)}")
        stream.notify_read(data)
      }
      else{ stream.close(); key.cancel() }
    }



  private def doAccept(key: SelectionKey, server: ServerSocketChannel){
    val channel = server.accept()
    assert(channel != null)//this shouldn't happen since the selector 
                           //tells us that we had a pending connection
    val stream = new Stream(UUID.randomUUID,key,channel.asInstanceOf[SocketChannel])
    //println(s"accepted new channel, assigning id ${stream.id.toString}")
    //make it non-blocking as well
    channel.configureBlocking(false);

    //register this channel with the event loop's selector and attach UUID to channel
    //all channels are monitored for READability 
    channel.register(selector, SelectionKey.OP_READ).attach(stream);
    acceptHandlers.foreach{_(stream)}
  }

  def shutdown(forceClose: Boolean = false)={
    allStreams.clear()
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
  def createServer(): Server = new Server()
}
