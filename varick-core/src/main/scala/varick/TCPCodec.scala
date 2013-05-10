package varick

import scala.concurrent.{Future,Promise}

trait ProtocolBuilder[T <: TCPCodec]{
  def build(conn: TCPConnection): T
}

/**
  * Represents a codec built on a TCP stream
  */
abstract class TCPCodec(val connection: TCPConnection) {

  type ProtocolData 

  /**
   * Encode the bytes and optionally return a ProtocolData message
   * @param bytes the bytes to process
   * @return a ProtocolData object at the discretion of the TCPCodec
   * @note Some(ProtocolData) indicates that the codec has accumulated
   *       enough bytes to encode a coherent ProtocolData object. None
   *       indicates that no ProtocolData object is discernable yet.
   */
  def process(bytes: Array[Byte]): Option[ProtocolData]

  //server asks whether codec wants to write
  def needs_write: Boolean = connection.needs_write

  //server tells us that a socket we previously requested to be monitored for
  //write capacity is now ready

  //add a handler function which will fire on new connections
  def onConnect(handler: Function1[TCPConnection,Unit]) = ()

  //case class WriteTracker(bytesToWrite: Int, promiseToFire: Promise[Unit])
  //schedule the data to be written
  //def write(data: Array[Byte]): Future[Unit] = {
  def write(data: Array[Byte]) = {
    if(data.length == 0){
      println("WARN: no data passed to write method")
    }
    //val wt = WriteTracker(data.length, Promise[Unit]())
    connection.write(data)
    //wt.promiseToFire.future
  }
}
