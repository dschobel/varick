package varick

object TCPBuilder extends ProtocolBuilder[BasicTCP]{
  override def build(conn: TCPConnection) = new BasicTCP(conn)
}

/**
  * a thin wrapper class which passes all reads and writes directly to the underlying TCP transport without any en/decoding
  */
class BasicTCP(c: TCPConnection) extends TCPCodec(c){
  type ProtocolData = Array[Byte]
  override def process(data: Array[Byte]) = Some(data)
}
