package varick.examples

import java.net.InetSocketAddress
//import varick.{TCPProtocol, Server}
import varick.http._


object HttpServer {

  def main(args: Array[String]): Unit = {
    var port = 8080
    if(args.length > 0) { port = args.head.toInt }


    val http = httpserver.createServer()

    println(s"listening on port $port")
    http.listen(new InetSocketAddress(port))
  }
}
