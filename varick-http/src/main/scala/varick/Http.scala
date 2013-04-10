package varick.http

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel}
import java.nio.ByteBuffer
import collection.mutable.ArrayBuffer
import org.apache.http.protocol.HttpDateGenerator
import varick._

class HTTPImpl() extends TCPProtocol {

  val readBuffer: ByteBuffer = ByteBuffer.allocate(16 * 1024)
  var connection: TCPConnection = null
  private val readHandlers: ArrayBuffer[Function2[TCPConnection,Array[Byte],Unit]] = ArrayBuffer()

  override def build(conn: TCPConnection): HTTPImpl ={
    val res = new HTTPImpl()
    res.connection = conn
    res
  }

  override def read(handlers: Seq[Function2[TCPProtocol, Array[Byte],Unit]]) = {
    connection.read match{
      case Some(data) => {
        readBuffer.put(data)
        val pos = readBuffer.position
        println(s"readBuffer.position: $pos")
        if(data.containsSlice(HTTPImpl.EOM)){
          readBuffer.flip
          val messageBytes = readBuffer.array.take(pos)
          readBuffer.clear()
          handlers.foreach{_(this,messageBytes)}
          readHandlers.foreach{_(connection,messageBytes)}
          connection.write(HTTPImpl.response.headerBytes)
          connection.write(HTTPImpl.response.content.getBytes)
          connection.close()
        }
      }
      case None => { println("WARN: didn't get any data") }
    }
  }
  override def write(bytes: Array[Byte]) = connection.write(bytes)
  override def needs_write = connection.needs_write
  override def onRead(handler: Function2[TCPConnection,Array[Byte],Unit]) = {
    readHandlers += handler
  }


}

object HTTPImpl {
  val response = StringResponse("hello from varick!")
  val CRLF: Array[Byte] = Array(13,10)
  val EOM = CRLF ++ CRLF
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
  def createServer(): TCPServer = new TCPServer(new HTTPImpl())
}
