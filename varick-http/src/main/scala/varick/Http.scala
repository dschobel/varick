package varick.http

import java.nio.ByteBuffer
import collection.mutable.ArrayBuffer
import org.apache.http.protocol.HttpDateGenerator
import varick._

class HTTPData extends Serializable {
  def toBytes(): Array[Byte] = Array()
}
object HTTPData{
  def apply(data: Array[Byte]) = new HTTPData()
}

object HTTPBuilder extends ProtocolBuilder[HTTPCodec]{
  override def build(conn: TCPConnection): HTTPCodec = new HTTPCodec(conn)
}

class HTTPCodec(connection: TCPConnection) extends TCPCodec(connection){

  type ProtocolData = HTTPData 

  val readBuffer: ByteBuffer = ByteBuffer.allocate(16 * 1024)

  override def read(handlers: Seq[Function2[TCPCodec, HTTPData,Unit]]) = {
    connection.read match{
      case Some(data) => {
        readBuffer.put(data)
        val pos = readBuffer.position
        //println(s"readBuffer.position: $pos")
        if(data.containsSlice(HTTPCodec.EOM)){
          readBuffer.flip
          val messageBytes = readBuffer.array.take(pos)
          readBuffer.clear()
          val http = HTTPData(messageBytes)
          handlers.foreach{_(this,http)}
        }
        else{
          println("incomplete request, suppressing read event")
        }
      }
      case None => { println("WARN: didn't get any data") }
    }
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
  def createServer() : TCPServer[HTTPCodec]= new TCPServer(HTTPBuilder)
}
