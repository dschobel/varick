package varick

import java.nio.ByteBuffer

object ByteBufferUtils{

  def append(existing: ByteBuffer, data: Array[Byte]): ByteBuffer = {
    if(data.length > existing.remaining()){
      println("ByteBufferUtils: write buffer needs to grow")
      val newSz = existing.position() + data.length
      val newbuffer = ByteBuffer.allocate(newSz)
      existing.flip()
      newbuffer.put(existing)
      newbuffer.put(data)
      newbuffer
    }
    else{
      println("ByteBufferUtils: appending data to existing existing without expansion")
      existing.put(data)
      existing
    }
  }
}
