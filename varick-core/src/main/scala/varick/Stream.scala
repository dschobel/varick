package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer

final class Stream(val id: UUID, private val socket: SocketChannel){
  private var dataHandlers: ArrayBuffer[Function1[Array[Byte],Unit]] = ArrayBuffer()
  val writeBuffer: ByteBuffer = ByteBuffer.allocate(1024) //1KB write-buffer per connection

  def close() = socket.close()
  def onData(dataHandler: Function1[Array[Byte],Unit]) = dataHandlers += dataHandler
  def notify_read(data: Array[Byte]) = dataHandlers.foreach{_(data)}
  def write(data: Array[Byte]): Long = {
      writeBuffer.put(data)
      //println(s"writeBuffer.position: ${writeBuffer.position}" )
      writeBuffer.flip()
      var written = 0
      if(writeBuffer.hasRemaining){
        written = socket.write(writeBuffer)
        println(s"server wrote $written bytes")
        writeBuffer.compact()
      }
      writeBuffer.flip
      println(s"writeBuffer.position: ${writeBuffer.position}" )
      written
  }
}
