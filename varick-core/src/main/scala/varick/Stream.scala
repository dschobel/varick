package varick

import java.net.InetSocketAddress
import java.nio.channels.{SocketChannel, ServerSocketChannel, Selector, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import collection.mutable.ArrayBuffer

final class Stream(val id: UUID, key: SelectionKey, socket: SocketChannel, initialWriteBufferSz : Int = 1024, maxWriteBufferSz: Int = 2 * 1024){
  assert(initialWriteBufferSz > -1)
  assert(maxWriteBufferSz > -1)
  assert(maxWriteBufferSz >= initialWriteBufferSz)

  private var dataHandlers: ArrayBuffer[Function1[Array[Byte],Unit]] = ArrayBuffer()
  var writeBuffer: ByteBuffer = ByteBuffer.allocate(initialWriteBufferSz) 

  def close() = socket.close()
  def onData(dataHandler: Function1[Array[Byte],Unit]) = dataHandlers += dataHandler
  def notify_read(data: Array[Byte]) = dataHandlers.foreach{_(data)}
  def needs_write = writeBuffer.position > 0
  def write()= {
    if(writeBuffer.position() > 0){
      println(s"write buffer has ${writeBuffer.position} bytes to write" )
      writeBuffer.flip()
      val written = socket.write(writeBuffer)
      writeBuffer.compact()
    }
    else{
      println("WARN: write buffer is empty. Passing empty buffers to write is probably not what you intended")
      0
    }

  }
  def write(data: Array[Byte])= {

    val newSz = writeBuffer.position() + data.length
    if(newSz > maxWriteBufferSz){ throw new java.nio.BufferOverflowException() }
  
    writeBuffer = ByteBufferUtils.append(writeBuffer,data)

    if(writeBuffer.position() > 0){
      println(s"write buffer has ${writeBuffer.position} bytes to write" )
      writeBuffer.flip()
      val written = socket.write(writeBuffer)
      println(s"server wrote $written bytes")
      writeBuffer.compact()
      if(needs_write){
        //if a socket needs to write, register interest    
        //with selector
        println("registering this socket for OP_WRITE availability")
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE)//.attach(this)
      }
      written
    }
    else{//write buffer is empty... should probably log a warning...
      println("WARN: write buffer is empty. Passing empty buffers to write is probably not what you intended")
      0
    }
  }
}
