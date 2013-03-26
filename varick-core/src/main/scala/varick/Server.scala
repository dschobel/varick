package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer

object Run extends App{
  println("hello world")
  val echo = net.createServer()
  echo.onAccept((stream: Stream) =>  stream.onData(stream.write(_)))
  echo.listen(new InetSocketAddress(3030))
}

final class Server(globalReadBufferSz: Int = 1024){

  private var serverChannel: ServerSocketChannel = _
  private var selector: Selector = _
  private var acceptHandlers: ArrayBuffer[Function1[Stream,Unit]] = ArrayBuffer()

  def socket = serverChannel.socket

  def onAccept(handler: Function1[Stream,Unit]) = acceptHandlers += handler

  def listen(address: InetSocketAddress, blocking: Boolean = true)={
    selector = Selector.open()
    val globalReadBuffer = ByteBuffer.allocate(globalReadBufferSz)

    serverChannel = ServerSocketChannel.open()
    serverChannel.socket().setReuseAddress(true) 
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


  private def tick(globalReadBuffer: ByteBuffer):Unit = {
    println("TICK")
    val available = selector.select() 

    assert(available > 0) //select blocks until we have channel activity
                          //so this is just a sanity check

    val keyset = selector.selectedKeys()
    println(s"keyset.size: ${keyset.size()}")
    val iter = keyset.iterator()
      while(iter.hasNext){
        val key = iter.next()
        iter.remove() //remove key from selection group
        if (key.isAcceptable){ doAccept(serverChannel) }
        else if (key.isReadable){ doRead(globalReadBuffer, key)}
    }
  }


  private def doRead(globalReadBuffer: ByteBuffer, key: SelectionKey){
      val stream = key.attachment.asInstanceOf[Stream]
      val channel = key.channel().asInstanceOf[SocketChannel]
      globalReadBuffer.clear()
      val bytesRead = channel.read(globalReadBuffer)
      if (bytesRead != -1) {
        globalReadBuffer.flip()
        val data = globalReadBuffer.array.take(bytesRead)
        println(s"server got: ${new String(data)}")
        stream.notify_read(data)
      }
      else{key.cancel()} //nothing to read, cancel registration
    }



  private def doAccept(server: ServerSocketChannel){
    println(s"is server blocking? ${server.isBlocking}")
    val channel = server.accept()
    assert(channel != null)//this shouldn't happen since the selector 
                           //tells us that we had a pending connection
    val stream = new Stream(UUID.randomUUID, channel)
    println(s"accepted new channel, assigning id ${stream.id.toString}")
    //make it non-blocking as well
    channel.configureBlocking(false);
    
    //register this channel with the event loop's selector and attach UUID to channel
    channel.register(selector, SelectionKey.OP_READ).attach(stream);
    acceptHandlers.foreach{_(stream)}
  }

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
  def createServer(): Server = new Server()
}
