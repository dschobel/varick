package varick


import java.nio.channels.spi.SelectorProvider

class NOOPSelectorProvider extends SelectorProvider{
  def openDatagramChannel(x$1: java.net.ProtocolFamily): java.nio.channels.DatagramChannel = ???
  def openDatagramChannel(): java.nio.channels.DatagramChannel = ???
  def openPipe(): java.nio.channels.Pipe = ???
  def openSelector(): java.nio.channels.spi.AbstractSelector = ???
  def openServerSocketChannel(): java.nio.channels.ServerSocketChannel = ???
  def openSocketChannel(): java.nio.channels.SocketChannel = ???
}
