package varick

import java.net.{InetSocketAddress,Socket}
import java.io.{PrintWriter,BufferedReader,InputStreamReader}
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import java.nio.ByteBuffer

import java.util.UUID
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.Timeouts._
import org.scalatest.concurrent.SocketInterruptor


class StreamTests extends FunSpec with BeforeAndAfter {

  describe("Stream") {


    it("fires event handlers defined in onData when notify_read is called") {
      val stream = new Stream(UUID.randomUUID,new TestSocketChannel())
      var count = 0
      stream.onData((_: Array[Byte]) => count +=1)
      assert(count === 0)
      stream.notify_read(Array())
      assert(count === 1)
      stream.notify_read(Array())
      assert(count === 2)
    }

    it("returns the number of bytes written") {
      val stream = new Stream(UUID.randomUUID,new TestSocketChannel())
      val data = "hello world".getBytes
      val Some(written) = stream.write(data)
      assert(written === data.length)
    }

    it("should throw an overflow exception if the buffer is not allowed to grow"){
      val stream = new Stream(UUID.randomUUID,new TestSocketChannel(), writeBufferSize = 2, allowWriteBufferToGrow = false)
       intercept[java.nio.BufferOverflowException] { stream.write("123".getBytes()) }
    }

    it("should let the buffer grow"){
      val stream = new Stream(UUID.randomUUID,new TestSocketChannel(), writeBufferSize = 2)
      stream.write("123".getBytes())
      assert(stream.writeBuffer.capacity === 3)
    }

    it("expands the write buffer without destroying existing buffered data"){
      val existing = ByteBuffer.allocate(10)
      existing.put("foo".getBytes)
      val newdata = "bar".getBytes
      val expanded = Stream.expandBuffer(existing, newdata)

      assert(expanded.position() === 6)
      expanded.flip()
      assert(expanded.get() === "f".getBytes.head)
      assert(expanded.get() === "o".getBytes.head)
      assert(expanded.get() === "o".getBytes.head)
      assert(expanded.get() === "b".getBytes.head)
      assert(expanded.get() === "a".getBytes.head)
      assert(expanded.get() === "r".getBytes.head)
      assert(expanded.hasRemaining() === false)
    }
  }
}
