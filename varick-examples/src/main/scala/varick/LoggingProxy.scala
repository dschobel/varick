package varick.examples

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import varick._
import collection.mutable.Map

object LoggingProxy {
  def main(args: Array[String]): Unit = {

    var port = 3030
    if(args.length > 0) { port = args.head.toInt }

    val connectionMap = Map[TCPConnection,TCPConnection]()
    val downstream_address = new InetSocketAddress(3031)

    implicit val proxy = net.createServer()

    proxy.onConnection{(conn) => {
        val sock = SocketChannel.open(downstream_address)
        sock.configureBlocking(false)
        val downstream: TCPConnection = conn flowTo(sock)
        println(s"proxy got a new connection from ${conn.id}, requests will be proxied to ${downstream.id}")
        connectionMap(conn) = downstream
      }
    }

    proxy.onRequest{(conn, data) => {
        val msg = new String(data)
        Console println s"server got: $msg from ${conn.id}" 
        val downstream = connectionMap(conn)  //look up the downstream endpoint
        //downstream notifyOnBytes(data.length) onComplete{_ => println("closing downstream..."); downstream close() }
        if(msg contains "close"){
          downstream close() 
          conn close() //clean up connections
          connectionMap remove conn //delete entry from connection map
        }
        else if (msg contains "shutdown"){
          proxy shutdown true
          System exit 0
        }
        else downstream write data //forward the message
      }
    }

    println(s"listening on port $port")
    proxy listen(new InetSocketAddress(port))
  }
}
