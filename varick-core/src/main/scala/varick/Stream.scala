package varick

import java.nio.channels.{SocketChannel, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import collection.mutable.ArrayBuffer

final class Stream(val id: UUID,
                       key: SelectionKey,
                       socket: SocketChannel,
                       initialWriteBufferSz : Int = 1024,
                       maxWriteBufferSz: Int = 2 * 1024)
 {
  assert(initialWriteBufferSz > -1)
  assert(maxWriteBufferSz > -1)
  assert(maxWriteBufferSz >= initialWriteBufferSz)

  var writeBuffer: ByteBuffer = ByteBuffer.allocate(initialWriteBufferSz)

  /**
   * close the Stream's underlying socket  
   */
  def close() ={ 
    //println(s"closing socket associated with stream $id")
    socket.close()
  }

  /**
   * indicate whether this stream has any data to write to its client
   */
  def needs_write = writeBuffer.position > 0

  /**
   *
   * @param data
   * @return
   */
  def write(data: Array[Byte]): Int= {
    if(writeBuffer.position() + data.length > maxWriteBufferSz){ throw new java.nio.BufferOverflowException() }
    writeBuffer = ByteBufferUtils.append(writeBuffer,data)
    write()
  }

  /**
   * writes the content of this stream's write buffer to the client 
   */
  def write():Int = {
    if(needs_write){
      //println(s"write buffer has ${writeBuffer.position} bytes to write" )
      writeBuffer.flip()
      var written = 0
      try{
        written = socket.write(writeBuffer)
      }catch{
        case ioe: java.io.IOException => { 
          println(s"ERROR: caught ${ioe.getMessage} when trying to write to $id.toString")
          socket.close()
          key.cancel()
          return -1
        }
      }
      writeBuffer.compact()
      if(needs_write){
        //if a socket needs to write more data, register interest with selector
        //println("registering this socket for OP_WRITE availability")
        val newops = key.interestOps() & SelectionKey.OP_WRITE
        key.interestOps(newops)
      }
      written
    }
    else{
      println("WARN: write buffer is empty. Calling write with empty buffers is probably not what you intended")
      0
    }
  }
}
