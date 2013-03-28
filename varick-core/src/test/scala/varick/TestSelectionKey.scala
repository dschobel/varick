package varick

import java.nio.channels.SelectionKey

class TestSelectionKey(val maxBytes: Int = -1) extends SelectionKey{
  def cancel(): Unit = ???
  def channel(): java.nio.channels.SelectableChannel = new TestSocketChannel(maxBytes)
  def interestOps(x$1: Int): java.nio.channels.SelectionKey = this
  def isValid(): Boolean = ???
  def interestOps(): Int = 0
  def readyOps(): Int = ???
  def selector(): java.nio.channels.Selector = ???
}
