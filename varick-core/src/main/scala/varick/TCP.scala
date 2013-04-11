package varick

import collection.mutable.ArrayBuffer

/**
  * Repesents a codec built on a TCP stream
  */
abstract class TCPCodec(val connection: TCPConnection) {

  //interface for server
  //def connectionMade(conn: TCPConnection) 
  //def dataReceived(conn: TCPConnection, data: Array[Byte])
  //def bytesToWrite(conn: TCPConnection): Array[Bytes]

  //server asks whether codec wants to write
  def needs_write: Boolean = connection.needs_write

  //server tells us that a socket we previously requested to be monitored for
  //write capacity is now ready
  def write(): Unit = connection.write()

  //server tells us a socket is readable, read some data and notify
  //the handlers if we have a complete protocol message
  def read(handlers: Seq[Function2[this.type, Array[Byte],Unit]])

  //interface for clients to interact with protocol
  //add a handler function which will fire on protocol messages
  def onRead(handler: Function2[TCPConnection,Array[Byte],Unit]) = ()

  //add a handler function which will fire on new connections
  def onConnect(handler: Function1[TCPConnection,Unit]) = ()

  //schedule the data to be written
  def write(bytes: Array[Byte]) = connection.write(bytes)
}


trait ProtocolBuilder[+T <: TCPCodec]{
  def build(conn: TCPConnection): T
}

object TCPBuilder extends ProtocolBuilder[BasicTCP]{
  override def build(conn: TCPConnection) = new BasicTCP(conn)
}

/**
  * a thin wrapper class which passes all reads and writes directly to the underlying TCP transport without any en/decoding
  */
class BasicTCP(c: TCPConnection) extends TCPCodec(c){

  private val readHandlers: ArrayBuffer[Function2[TCPConnection,Array[Byte],Unit]] = ArrayBuffer()

  override def read(handlers: Seq[Function2[this.type, Array[Byte],Unit]]) = {
    connection.read match{
      case Some(data) => {
        handlers.foreach{_(this,data)}
        readHandlers.foreach{_(connection,data)}
      }
      case None => ()
    }
  }
  override def onRead(handler: Function2[TCPConnection,Array[Byte],Unit]) = {
    println("adding handlers")
    readHandlers += handler
  }
}
