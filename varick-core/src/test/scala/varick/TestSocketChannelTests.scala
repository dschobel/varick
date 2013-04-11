package varick

import java.nio.ByteBuffer

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


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
      val raw = "foo".getBytes()
      val buffer = ByteBuffer.wrap(raw)
      val bytesToWrite = 1
      val testSocket = new TestSocketChannel(bytesToWrite)
      val written = testSocket.write(buffer)
      assert(written === bytesToWrite)
      assert(buffer.position === bytesToWrite)
    }

    it("should only write as many bytes as specified #2") {
      val raw = "foo".getBytes()
      val buffer = ByteBuffer.wrap(raw)
      val bytesToWrite = 2
      val testSocket = new TestSocketChannel(bytesToWrite) 
      val written = testSocket.write(buffer)
      assert(written === bytesToWrite)
      assert(buffer.position === bytesToWrite)
    }
  }
}
