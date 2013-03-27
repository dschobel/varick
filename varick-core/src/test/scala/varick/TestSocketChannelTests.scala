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


class TestSocketChannelTests extends FunSpec with BeforeAndAfter {

  describe("TestSocketChannel") {
    it("should write all data by default") {
      val testSocket = new TestSocketChannel()
      val raw = "foo".getBytes()
      val buffer = ByteBuffer.wrap(raw)
      val written = testSocket.write(buffer)
      assert(written === raw.length)
      assert(buffer.position === buffer.limit)
    }

    it("should only write as many bytes as specified") {
      val testSocket = new TestSocketChannel()
      val raw = "foo".getBytes()
      val buffer = ByteBuffer.wrap(raw)
      val bytesToWrite = 1
      val written = testSocket.write(buffer,bytesToWrite)
      assert(written === bytesToWrite)
      assert(buffer.position === bytesToWrite)
    }

    it("should only write as many bytes as specified #2") {
      val testSocket = new TestSocketChannel() 
      val raw = "foo".getBytes()
      val buffer = ByteBuffer.wrap(raw)
      val bytesToWrite = 2
      val written = testSocket.write(buffer,bytesToWrite)
      assert(written === bytesToWrite)
      assert(buffer.position === bytesToWrite)
    }
  }
}
