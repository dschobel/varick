package varick

import java.nio.ByteBuffer

object ByteBufferUtils{

  def append(existing: ByteBuffer, data: Array[Byte]): ByteBuffer = {
    if(data.length > existing.remaining()){
      val newbuffer = ByteBuffer.allocate(existing.position() + data.length)
      existing.flip()
      newbuffer.put(existing)
      newbuffer.put(data)
      newbuffer
    }
    else{
      existing.put(data)
      existing
    }
  }
}
