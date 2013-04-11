package varick


import java.nio.channels.Selector

class NOOPSelector extends Selector {
  def openDatagramChannel(x$1: java.net.ProtocolFamily): java.nio.channels.DatagramChannel = ???
  def openDatagramChannel(): java.nio.channels.DatagramChannel = ???
  def openPipe(): java.nio.channels.Pipe = ???
  def openSelector(): java.nio.channels.spi.AbstractSelector = ???
  def openServerSocketChannel(): java.nio.channels.ServerSocketChannel = ???
  def openSocketChannel(): java.nio.channels.SocketChannel = ???

  def close(): Unit = ???
  def isOpen(): Boolean = ???
  def keys(): java.util.Set[java.nio.channels.SelectionKey] = ???
  def provider(): java.nio.channels.spi.SelectorProvider = ???

  def select(): Int = ???
  def select(x$1: Long): Int = ???
  def selectNow(): Int = ???

  def selectedKeys(): java.util.Set[java.nio.channels.SelectionKey] = ???
  def wakeup(): java.nio.channels.Selector = ???
}
