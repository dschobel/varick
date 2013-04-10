package varick.http

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel}
import java.nio.ByteBuffer
import collection.mutable.ArrayBuffer
import org.apache.http.protocol.HttpDateGenerator
import varick._

object HTTPImpl extends TCPProtocol{
  val response = StringResponse("hello form varick!")
  val CRLF: Array[Byte] = Array(13,10)
  val EOM = CRLF ++ CRLF

  private var stream: Stream = null;

  override def connectionMade(stream: Stream) = this.stream = stream 

  override def onRead(handler: Function2[Stream,Array[Byte],Unit]) = ???

  override def dataReceived(stream: Stream, data: Array[Byte]) ={
    if(data.containsSlice(EOM)){
      stream.write(response.headerBytes)
      stream.write(response.content.getBytes)
      stream.close()
    }
    else{
      //buffer message
    }
  }
}

case class StringResponse(content: String) {
  val responseStatus = "HTTP/1.1 200 OK\r\n"
  val headers: List[Pair[String,String]] = List(
    ("Content-Length",content.length.toString),
    ("Content-Type", "text/plain; charset=\"utf8\""),
    ("Date", StringResponse.DateGenerator.getCurrentDate())
  )

  def allHeaders = headers.map{case(k,v) => k +": " + v}.mkString("\r\n") + "\r\n"
  def headerBytes: Array[Byte] = (responseStatus + allHeaders + "\r\n").getBytes
}

object StringResponse{
  val DateGenerator = new HttpDateGenerator() 
}

object httpserver {
  def createServer(): Server = new Server(HTTPImpl)
}
