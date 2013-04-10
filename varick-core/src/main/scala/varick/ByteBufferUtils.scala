package varick

import java.nio.ByteBuffer

object ByteBufferUtils{

  /**
   * append will append data to an existing byte buffer
   * @param existing the buffer to which data will be appended
   * @param data the data to append
   * @return a ByteBuffer containing all of the data in 'existing' buffer with bytes of 'data' appended
   * @note this method _might_ mutate the passed in byte buffer. The only way you get a new bytebuffer is if the existing one
   * has insufficient capacity. This semantic is kind of messy but the alternative entails needlessly copying of buffer data.
   */
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
