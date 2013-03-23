package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

final class Stream(val id: UUID){
  

}


final class Server(){

  private var serverChannel: ServerSocketChannel = _
  private var selector: Selector = _

  def listen(address: InetSocketAddress, blocking: Boolean = true, tickOnce: Boolean = false)={
    serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    serverChannel.socket().bind(address)

    selector = Selector.open()
    serverChannel.register(selector, SelectionKey.OP_ACCEPT)

    if (blocking){
      while(true && !tickOnce){ tick() }
    }
    else{//don't block thread (for easier testing)
      Future{  while(true && !tickOnce){tick()}  }
    }
  }

  def shutdown()={
    //TODO: close all channels
    serverChannel.close()
    selector.close()
  }

  private def tick() = {
    val available = selector.select() 

      assert(available > 0) //select blocks until we have channel activity so
                            //this is just a sanity check

    val iter = selector.selectedKeys().iterator()
      while(iter.hasNext){
        val key = iter.next()
        iter.remove

        if (key.isReadable){ doRead(key)}
        else if(key.isWritable){ doWrite(key)}
        else if(key.isAcceptable){doAccept(serverChannel.accept()) }
    }
  }

  private def doRead(key: SelectionKey){
      //println(s"${key.attachment.toString} is READable!")
  }

  private def doWrite(key: SelectionKey){
      //println(s"${key.attachment.toString} is WRITEable!")
  }

  private def doAccept(channel: SocketChannel){
    val uuid = UUID.randomUUID
    println(s"accepting new channel, assigning id ${uuid.toString}")
    //make it non-blocking as well
    channel.configureBlocking(false);
    
    //register this channel with the event loop's selector and attach UUID to channel
    channel.register(selector, SelectionKey.OP_READ |SelectionKey.OP_WRITE).attach(uuid);
  }
}

object net {
  def createServer(): Server = new Server()
}
