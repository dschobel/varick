package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer

final class Stream(val id: UUID){
  private var dataHandlers: ArrayBuffer[Function1[Array[Byte],Unit]] = ArrayBuffer()
  var writeBuffer: Seq[Byte] = Nil

  def clearWriteBuffer() = writeBuffer = Nil
  def onData(dataHandler: Function1[Array[Byte],Unit]) = dataHandlers += dataHandler
  def notify_read(data: Array[Byte]) = dataHandlers.foreach{_(data)}
  def writePending = writeBuffer.size != 0
  def write(data: Seq[Byte]) = writeBuffer = writeBuffer ++ data
}


final class Server(){

  private var serverChannel: ServerSocketChannel = _
  private var selector: Selector = _
  private var acceptHandlers: ArrayBuffer[Function1[Stream,Unit]] = ArrayBuffer()

  def socket = serverChannel.socket

  def onAccept(handler: Function1[Stream,Unit]) = acceptHandlers += handler

  def listen(address: InetSocketAddress, blocking: Boolean = true)={
    serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(address)
    serverChannel.socket().setReuseAddress(true) 

    selector = Selector.open()
    serverChannel.register(selector, SelectionKey.OP_ACCEPT)


    val readBuffer: ByteBuffer = ByteBuffer.allocate(16 * 1024) //16kb read buffer

    if (blocking){
      while(true){ tick(readBuffer) }
    }
    else{//don't block thread (for easier testing)
      Future{  while(true){tick(readBuffer)}  }
    }
  }

  def shutdown()={
    serverChannel.socket().close()
    serverChannel.close()
    val iter = selector.keys().iterator()
    while(iter.hasNext){
        val key = iter.next()
        //iter.remove
        key.cancel()
        key.channel().close()
    }
    selector.close()
  }

  private def tick(readBuffer: ByteBuffer) = {
    val available = selector.select() 

    assert(available > 0) //select blocks until we have channel activity so
                          //this is just a sanity check

    val iter = selector.selectedKeys().iterator()
      while(iter.hasNext){
        val key = iter.next()
        iter.remove
        if (key.isReadable){ doRead(readBuffer, key)}
        else if(key.isWritable){ doWrite(key)}
        else if(key.isAcceptable){ doAccept(serverChannel.accept()) }
    }
  }


  private def doRead(readBuffer: ByteBuffer, key: SelectionKey){
      val stream = key.attachment.asInstanceOf[Stream]
      //println(s"${stream.id.toString} is READable!")
      val client = key.channel().asInstanceOf[SocketChannel]
      readBuffer.clear()
      if (client.read(readBuffer) != -1) {
        readBuffer.flip()
        stream.notify_read(readBuffer.array)
        }else{ key.cancel();}
    }

  private def doWrite(key: SelectionKey){
      val stream = key.attachment.asInstanceOf[Stream]
      //println(s"${stream.id.toString} is WRITEable!")
  }

  private def writeStream(stream: Stream){
    assert(stream.writePending)
    //write_nonblock(stream.writequeue)
    //stream.clearQueue
  }

  private def doAccept(channel: SocketChannel){
    val stream = new Stream(UUID.randomUUID)
    //println(s"accepting new channel, assigning id ${stream.id.toString}")
    //println(s"acceptHandlers.size: ${acceptHandlers.size}")
    //make it non-blocking as well
    channel.configureBlocking(false);
    
    //register this channel with the event loop's selector and attach UUID to channel
    channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE).attach(stream);
    acceptHandlers.foreach{_(stream)}
  }
}

object net {
  def createServer(): Server = new Server()
}
