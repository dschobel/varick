package varick

import java.net.InetSocketAddress
import java.nio.channels.{ServerSocketChannel, Selector, SelectionKey}
import scala.concurrent._
import ExecutionContext.Implicits.global

final class Server(){

  def listen(address: InetSocketAddress, blocking: Boolean = true): Unit ={
    val channel = ServerSocketChannel.open()
    channel.configureBlocking(false)
    channel.socket().bind(address)

    //val selector = Selector.open()
    //channel.register(selector, SelectionKey.OP_ACCEPT)
//                              |SelectionKey.OP_CONNECT
 //                             |SelectionKey.OP_READ
   //                           |SelectionKey.OP_WRITE)

    if (blocking){
      while(true){ tick() }
    }
    else{//don't block thread (to make testing easier)
      Future{  while(true){tick()}}
    }
  }

  def tick():Unit = println("reactor loop tick!")
  
}

object net {
  def createServer(): Server = new Server()
}

