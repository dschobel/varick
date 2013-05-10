package varick

import java.nio.channels.{SocketChannel, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID

object TCPConnection{
  val GlobalReadBufferSz: Int = 1024
  val GlobalReadBuffer = ByteBuffer.allocate(GlobalReadBufferSz)
}

/**
  * State and errorhandling for a connected TCP socket
  */
class TCPConnection (val id: UUID, 
                     val key: SelectionKey, 
                         socket: SocketChannel, 
                         initialWriteBufferSz : Int = 1024, 
                         maxWriteBufferSz: Int = 16 * 1024) {

  assert(initialWriteBufferSz > 0)
  assert(maxWriteBufferSz > 0)
  assert(maxWriteBufferSz >= initialWriteBufferSz)

  var writeBuffer: ByteBuffer = ByteBuffer.allocate(initialWriteBufferSz)
  val downStream = collection.mutable.ArrayBuffer[SelectionKey]()

  /**
   * close the TCPConnection's underlying socket  
   */
  def close() = socket.close()

  /**
   * flowTo make network congestion from sink propagate this connection
   * @param sink the down-stream TCPConnection
   */
  def flowTo(sink: TCPConnection):Unit = downStream += sink.key

  /** 
   * Reads data from this TCP socket
   */
  //def read(handlers: Seq[Function2[TCPConnection,Array[Byte],Unit]]) = {
  def read(): Option[Array[Byte]] = {
      var bytesRead = 0
      TCPConnection.GlobalReadBuffer.clear()
      try{
        bytesRead = socket.read(TCPConnection.GlobalReadBuffer)
      } catch{ 
        case ioe: java.io.IOException => { 
          println(s"caught ${ioe.getMessage}, closing socket and cancelling key")
          socket.close(); 
          key.cancel();
          return None
        }
      }
      if (bytesRead >= 0) {
        TCPConnection.GlobalReadBuffer.flip()
        val data = TCPConnection.GlobalReadBuffer.array.take(bytesRead)
        return Some(data)

        //protocol.dataReceived(conn,data)
        //handlers.foreach{_(this,data)}
        //println(s"server got: ${new String(data)}")
      }
      else{ socket.close(); return None }
  }

  /**
   * indicate whether this conn has any data to write to its client
   */
  def needs_write = writeBuffer.position > 0

  /**
   * write
   * @param data the bytes to write to the socket
   * @return the number of bytes written
   */
  def write(data: Array[Byte]): Int = {
    if(writeBuffer.position() + data.length > maxWriteBufferSz){ throw new java.nio.BufferOverflowException() }
    writeBuffer = ByteBufferUtils.append(writeBuffer,data)
    writeBuffered()
  }

  /**
   * Non-blocking write for the content of this connections's write buffer
   */
  def writeBuffered(): Int = {
    if(needs_write){
      //println(s"write buffer has ${writeBuffer.position} bytes to write" )
      writeBuffer.flip()
      var written = 0
      try{
        written = socket.write(writeBuffer)
      }catch{
        case ioe: java.io.IOException => { 
          println(s"ERROR: caught ${ioe.getMessage} when trying to write to $id\n\n${ioe.getStackTrace.take(6).mkString("\n")}")
          socket.close()
          key.cancel()
          return -1
        }
      }
      writeBuffer.compact()

      //if a socket needs to write more data, register interest with selector
      val writeInterest = if (needs_write) SelectionKey.OP_WRITE else ~SelectionKey.OP_WRITE
      key.interestOps(key.interestOps() & writeInterest)
      written
    }
    else{
      println("WARN: write buffer is empty. Calling writeBuffered with empty buffers is probably not what you intended")
      0
    }
  }
}
