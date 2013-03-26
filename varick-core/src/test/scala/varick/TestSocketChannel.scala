package varick

import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import java.nio.ByteBuffer


class TestSocketChannel(val bytesToWrite: Int = -1) extends SocketChannel(new NOOPSelectorProvider()){
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
    println("writing in test socket channel!")
    println("only $bytesToWrite bytes will be written")
    var res = bytesToWrite
    if(bytesToWrite == -1){
      res = buffer.limit
    }

    buffer.clear() //TODO: move marker correct amount
    res
  }
}
