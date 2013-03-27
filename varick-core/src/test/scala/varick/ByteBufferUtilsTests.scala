package varick

import java.nio.ByteBuffer

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.Timeouts._
import org.scalatest.concurrent.SocketInterruptor

class ByteBufferUtilsTests extends FunSpec with BeforeAndAfter {

  def toByteArray(range: Range) = (range.map{_.asInstanceOf[Byte]}).toArray

  describe("append") {
    it("returns the original byte buffer if there was suffifcient capacity") {
      val original = ByteBuffer.allocate(10)
      val res = ByteBufferUtils.append(original, "foo".getBytes)
      assert(res eq original)
    }

    it("returns the original byte buffer if there was no data to append") {
      val original = ByteBuffer.allocate(10)
      val nada: Array[Byte] = Array()
      val res = ByteBufferUtils.append(original, nada)
      assert(res eq original)
    }

    it("returns a new ByteBuffer which is large enough to accomodate appended data") {
      val original = ByteBuffer.allocate(10)
      val sizeOfNewData = 20
      val newBytes = toByteArray((1 to sizeOfNewData))
      val res = ByteBufferUtils.append(original, newBytes)
      assert(res ne original)
      assert(res.capacity ==sizeOfNewData)
    }

    it("reuses existing buffer and preserve existing data") {
      val original = toByteArray(1 to 5)
      val newdata = toByteArray(6 to 8)

      val origBuffer = ByteBuffer.allocate(10)
      origBuffer.put(original) //so we have a byte buffer of capacity 10
                               //with data in indexes 0 -> 4
      val res = ByteBufferUtils.append(origBuffer, newdata)
      assert(res eq origBuffer)
      assert(res.position() === 8)
      res.flip()
      assert(res.get() === 1)
      assert(res.get() === 2)
      assert(res.get() === 3)
      assert(res.get() === 4)
      assert(res.get() === 5)
      assert(res.get() === 6)
      assert(res.get() === 7)
      assert(res.get() === 8)
      assert(res.hasRemaining() === false)
    }

    it("preserves existing data when expanding a buffer") {
      val original = toByteArray(1 to 5)
      val newdata = toByteArray(6 to 10)

      val origBuffer = ByteBuffer.allocate(7)
      origBuffer.put(original) //so we have a byte buffer of capacity 7
                               //with data in indexes 0 -> 4
      val res = ByteBufferUtils.append(origBuffer, newdata)
      //and after the append we should have a new ByteBuffer with capacity 10 
      //and values 1->10 in the first ten slots
      assert(res ne origBuffer)
      assert(res.position() === 10)
      assert(res.capacity() === 10)
      res.flip()
      assert(res.get() === 1)
      assert(res.get() === 2)
      assert(res.get() === 3)
      assert(res.get() === 4)
      assert(res.get() === 5)
      assert(res.get() === 6)
      assert(res.get() === 7)
      assert(res.get() === 8)
      assert(res.get() === 9)
      assert(res.get() === 10)
      assert(res.hasRemaining() === false)
    }
  }
}
