package varick


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

  /**
   * does this codec instance have writable data buffered?
   * @return true if the underlying connection has writable data buffered, else false
   */
  def needs_write: Boolean = connection.needs_write

  /**
   * add a handler function which will fire on new connections
   */
  def onConnect(handler: Function1[TCPConnection,Unit]) = ()

  /**
   * execute a non-blocking write
   */
  def write(data: Array[Byte]) = {
    if(data.length == 0){
      println("WARN: no data passed to write method")
    }
    connection.write(data)
  }
}
