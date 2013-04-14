package varick

import collection.mutable.ArrayBuffer

trait ProtocolBuilder[T <: TCPCodec[_]]{
  def build(conn: TCPConnection): T
}

/**
  * Represents a codec built on a TCP stream
  */
abstract class TCPCodec[D](val connection: TCPConnection) {

  //type ProtocolData

  //interface for server
  //def connectionMade(conn: TCPConnection) 
  //def dataReceived(conn: TCPConnection, data: Array[Byte])
  //def bytesToWrite(conn: TCPConnection): Array[Bytes]

  //server asks whether codec wants to write
  def needs_write: Boolean = connection.needs_write

  //server tells us that a socket we previously requested to be monitored for
  //write capacity is now ready
  def write(): Unit = connection.write()

  //server tells the codec that a socket is readable, 
  //we read some data and selectively notify
  //the handlers if we have a complete protocol message
  def read(handlers: Seq[Function2[TCPCodec[D], D,Unit]])

  //add a handler function which will fire on new connections
  def onConnect(handler: Function1[TCPConnection,Unit]) = ()

  //schedule the data to be written
  def write(bytes: Array[Byte]) = connection.write(bytes)
}

