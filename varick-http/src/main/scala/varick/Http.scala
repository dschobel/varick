package varick.http

import java.nio.ByteBuffer
import collection.mutable.ArrayBuffer
import org.apache.http.protocol.HttpDateGenerator
import varick._

object HTTPBuilder extends ProtocolBuilder[HTTPCodec]{
  override def build(conn: TCPConnection): HTTPCodec = new HTTPCodec(conn)
}

class HTTPCodec(connection: TCPConnection) extends TCPCodec(connection){
  val readBuffer: ByteBuffer = ByteBuffer.allocate(16 * 1024)
  private val readHandlers: ArrayBuffer[Function2[TCPConnection,Array[Byte],Unit]] = ArrayBuffer()

  override def read(handlers: Seq[Function2[this.type, Array[Byte],Unit]]) = {
    connection.read match{
      case Some(data) => {
        readBuffer.put(data)
        val pos = readBuffer.position
        //println(s"readBuffer.position: $pos")
        if(data.containsSlice(HTTPCodec.EOM)){
          readBuffer.flip
          val messageBytes = readBuffer.array.take(pos)
          readBuffer.clear()
          handlers.foreach{_(this,messageBytes)}
          readHandlers.foreach{_(connection,messageBytes)}
        }
        else{
          println("incomplete request, suppressing read event")
        }
      }
      case None => { println("WARN: didn't get any data") }
    }
  }
  override def onRead(handler: Function2[TCPConnection,Array[Byte],Unit]) = {
    readHandlers += handler
  }
}

object HTTPCodec {
  val CRLF: Array[Byte] = Array(13,10)
  val EOM = CRLF ++ CRLF

  def StringResponder(content: String, connection: TCPConnection){
    val response = StringResponse(content)
    connection.write(response.headerBytes)
    connection.write(response.content.getBytes)
    connection.close()
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
  def createServer() = new TCPServer(HTTPBuilder)
}
