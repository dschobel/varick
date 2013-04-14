package varick.examples

import java.util.Date
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import varick.TCPCodec
import varick.http._


object HttpServer {

  def main(args: Array[String]): Unit = {
    var port = 8080
    if(args.length > 0) { port = args.head.toInt }


    val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    val http = httpserver.createServer()

    http.onRead((c, d) => {
        HTTPCodec.StringResponder(s"hello world, it is currently ${dateFormat.format(new Date)}\n", c.connection)
        c.connection.close()
      })

    println(s"listening on port $port")
    http.listen(new InetSocketAddress(port))
  }
}
