package varick.examples

import java.net.{InetSocketAddress,Socket}
import java.nio.channels.SocketChannel
import varick._
import collection.mutable.Map

object LoggingProxy {
  def main(args: Array[String]): Unit = {

    var port = 3030
    if(args.length > 0) { port = args.head.toInt }

    val connectionMap = Map[TCPConnection,TCPConnection]()
    val downstream_address = new InetSocketAddress(3031)

    val proxy = net.createServer()

    proxy.onConnection{(conn) => {
        val sock = SocketChannel.open(downstream_address)
        sock.configureBlocking(false)
        val downstream = proxy.monitor(sock)
        println(s"proxy got a new connection from ${conn.id}, requests will be proxied to ${downstream.id}")
        conn flowTo downstream
        connectionMap(conn) = downstream
      }
    }

    proxy.onRequest{(conn, data) => {
        println(s"server got: ${new String(data)} from ${conn.id}")
        val downstream: TCPConnection = connectionMap(conn)  //look up the downstream endpoint
        downstream write(data) //proxy the request
        conn close() //clean up connections
        downstream close() 
        connectionMap remove conn //delete entry from connection map
      }
    }

    println(s"listening on port $port")
    proxy listen(new InetSocketAddress(port))
  }
}
