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

  class NoopSelectorProvider extends SelectorProvider{
    def openDatagramChannel(x$1: java.net.ProtocolFamily): java.nio.channels.DatagramChannel = ???
    def openDatagramChannel(): java.nio.channels.DatagramChannel = ???
    def openPipe(): java.nio.channels.Pipe = ???
    def openSelector(): java.nio.channels.spi.AbstractSelector = ???
    def openServerSocketChannel(): java.nio.channels.ServerSocketChannel = ???
    def openSocketChannel(): java.nio.channels.SocketChannel = ???
  }

  class NoopChannel extends SocketChannel(new NoopSelectorProvider()){
    def implCloseSelectableChannel(): Unit = ???
    def implConfigureBlocking(x$1: Boolean): Unit = ???

    // Members declared in java.nio.channels.NetworkChannel
    def getLocalAddress(): java.net.SocketAddress = ???
    def getOption[T](x$1: java.net.SocketOption[T]): T = ???
    def supportedOptions(): java.util.Set[java.net.SocketOption[_]] = ???

    // Members declared in java.nio.channels.SocketChannel
    def bind(x$1: java.net.SocketAddress): java.nio.channels.SocketChannel = ???
    def connect(x$1: java.net.SocketAddress): Boolean = ???
    def finishConnect(): Boolean = ???
    def getRemoteAddress(): java.net.SocketAddress = ???
    def isConnected(): Boolean = ???
    def isConnectionPending(): Boolean = ???
    def read(x$1: Array[java.nio.ByteBuffer],x$2: Int,x$3: Int): Long = ???
    def read(x$1: java.nio.ByteBuffer): Int = ???
    def setOption[T](x$1: java.net.SocketOption[T],x$2: T): java.nio.channels.SocketChannel = ???
    def shutdownInput(): java.nio.channels.SocketChannel = ???
    def shutdownOutput(): java.nio.channels.SocketChannel = ???
    def socket(): java.net.Socket = ???
    def write(x$1: Array[java.nio.ByteBuffer],x$2: Int,x$3: Int): Long = 0
    def write(buffer: java.nio.ByteBuffer): Int = {
      println("writing in noopchannel!")
      val res = buffer.position
      println(s"remaining: $res")
      buffer.clear()
      res
    }
  }

  describe("Stream") {

    var stream: Stream = null

    before{
      stream = new Stream(UUID.randomUUID,new NoopChannel())
    }

    it("fires event handlers defined in onData when notify_read is called") {
      var count = 0
      stream.onData((_: Array[Byte]) => count +=1)
      assert(count === 0)
      stream.notify_read(Array())
      assert(count === 1)
      stream.notify_read(Array())
      assert(count === 2)
    }

    it("it writes data until the buffer is empty") {
      val data = "hello world".getBytes
      val written = stream.write(data)
      assert(stream.writeBuffer.position === 0)
      //assert(written === data.length)
    }
  }
}
