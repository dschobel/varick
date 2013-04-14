package varick

trait ProtocolBuilder[T <: TCPCodec]{
  def build(conn: TCPConnection): T
}

/**
  * Represents a codec built on a TCP stream
  */
abstract class TCPCodec(val connection: TCPConnection) {

  type ProtocolData 
  //interface for server
  //def connectionMade(conn: TCPConnection) 
  //def dataReceived(conn: TCPConnection, data: Array[Byte])
  //def bytesToWrite(conn: TCPConnection): Array[Bytes]

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
  def write(): Unit = connection.write()

  //add a handler function which will fire on new connections
  def onConnect(handler: Function1[TCPConnection,Unit]) = ()

  //schedule the data to be written
  def write(data: Array[Byte]) = connection.write(data)
}
