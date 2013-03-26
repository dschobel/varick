package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer

object Stream{
  def expandBufferIfNeeded(buffer: ByteBuffer, newdata: Array[Byte], expansionAllowed: Boolean = true):ByteBuffer={
    if(newdata.length > buffer.remaining()){
      println("write buffer needs to grow")
      if(false == expansionAllowed){ throw new java.nio.BufferOverflowException() }
       Stream.expandBuffer(buffer, newdata)
    }
    else{
      println("appending data to existing write buffer")
      buffer.put(newdata)
    }
  }
  def expandBuffer(existing: ByteBuffer, newdata: Array[Byte]): ByteBuffer ={
      val newSz = existing.position() + newdata.length
      val newbuffer = ByteBuffer.allocate(newSz)
      existing.flip()
      newbuffer.put(existing)
      newbuffer.put(newdata)
      newbuffer
  }
}
final class Stream(val id: UUID, private val socket: SocketChannel, writeBufferSize: Int = 1024, allowWriteBufferToGrow: Boolean = true){
  private var dataHandlers: ArrayBuffer[Function1[Array[Byte],Unit]] = ArrayBuffer()
  var writeBuffer: ByteBuffer =  ByteBuffer.allocate(writeBufferSize)

  def close() = socket.close()
  def onData(dataHandler: Function1[Array[Byte],Unit]) = dataHandlers += dataHandler
  def notify_read(data: Array[Byte]) = dataHandlers.foreach{_(data)}
  def needs_write() = writeBuffer.position > 0
  def write(data: Array[Byte]): Option[Int] = {

    writeBuffer = Stream.expandBufferIfNeeded(writeBuffer,data, allowWriteBufferToGrow)

    if(writeBuffer.position() > 0){
      println(s"write buffer has ${writeBuffer.position} bytes to write" )
      writeBuffer.flip()
      val written = socket.write(writeBuffer)
      println(s"server wrote $written bytes")
      writeBuffer.compact()
      Some(written)
    }
    else{//write buffer is empty...
      println("write buffer is empty...")
      None
    }
  }
}
