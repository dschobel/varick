package varick.examples

import java.net.InetSocketAddress
import varick._

object EchoServer {
  def main(args: Array[String]): Unit = {
    var port = 3030
    if(args.length > 0) { port = args.head.toInt }

    val echo = net.createServer()
    echo.onRead{(conn, data) => conn.write(data);conn.connection.close()}

    println(s"listening on port $port")
    echo.listen(new InetSocketAddress(port))
  }
}
