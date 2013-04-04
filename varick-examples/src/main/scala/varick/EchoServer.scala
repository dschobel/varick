package varick.examples

import java.net.InetSocketAddress
import varick._

object EchoServer {
  def main(args: Array[String]): Unit = {
    var port = 3030
    if(args.length > 0) { port = args.head.toInt }


    val echo = net.createServer()
    echo.onAccept((stream) => 
                      stream.onData{ (data) => 
                        {
                          stream.write(data)
                        }})

    println(s"listening on port $port")
    echo.listen(new InetSocketAddress(port))
  }
}
