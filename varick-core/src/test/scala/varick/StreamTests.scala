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
      val stream = new Stream(UUID.randomUUID, new TestSelectionKey(), new TestSocketChannel())
      var count = 0
      stream.onData((_: Array[Byte]) => count +=1)
      assert(count === 0)
      stream.notify_read(Array())
      assert(count === 1)
      stream.notify_read(Array())
      assert(count === 2)
    }

    it("returns the number of bytes written") {
      val stream = new Stream(UUID.randomUUID, new TestSelectionKey(), new TestSocketChannel())
      val data = "hello world".getBytes
      val written = stream.write(data)
      assert(written === data.length)
    }

    it("should throw an overflow exception maxWriteBufferSz is exceeded"){
      val stream = new Stream(UUID.randomUUID,new TestSelectionKey(),new TestSocketChannel(), initialWriteBufferSz = 2, maxWriteBufferSz = 2)
       intercept[java.nio.BufferOverflowException] { stream.write("123".getBytes()) }
    }

    it("should let the buffer grow when maxWriteBufferSz is not exceeded"){
      val stream = new Stream(UUID.randomUUID,new TestSelectionKey(), new TestSocketChannel(), initialWriteBufferSz = 2)
      stream.write("123".getBytes())
      assert(stream.writeBuffer.capacity === 3)
    }
    it("stream doesn't need_write when a write consumes entire write buffer"){
      //configure a stream backed by a socket which only writes 2 bytes per write
      val stream = new Stream(UUID.randomUUID,new TestSelectionKey(), new TestSocketChannel(), initialWriteBufferSz = 2)
      stream.write("123".getBytes())
      assert(false === stream.needs_write)
    }

    it("stream needs_write when a write doesn't consume entire write buffer"){
      //configure a stream backed by a socket which only writes 2 bytes per write
      val stream = new Stream(UUID.randomUUID,new TestSelectionKey(2), new TestSocketChannel(2), initialWriteBufferSz = 2)
      stream.write("123".getBytes())
      assert(stream.needs_write)
      stream.writeBuffer.flip()
      //confirm that "3" (or the byte equivalent thereof) is the 
      //next byte to be written
      assert(stream.writeBuffer.get() === "3".getBytes.head)
    }
  }
}
