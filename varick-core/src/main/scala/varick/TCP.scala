package varick

object TCPBuilder extends ProtocolBuilder[BasicTCP]{
  override def build(conn: TCPConnection) = new BasicTCP(conn)
}

/**
  * a thin wrapper class which passes all reads and writes directly to the underlying TCP transport without any en/decoding
  */
class BasicTCP(c: TCPConnection) extends TCPCodec(c){

  class WrappedByteArray(val repr: Array[Byte]) extends Serializable{
    def toBytes() = repr
  }

  type ProtocolData = WrappedByteArray

  implicit def byteToArrayWrapper(bytes: Array[Byte]): WrappedByteArray = new WrappedByteArray(bytes)

  implicit def wrappedArrayToBytes(wrapped: WrappedByteArray): Array[Byte] = wrapped.toBytes()

  override def read(handlers: Seq[Function2[TCPCodec, WrappedByteArray,Unit]]) = {
    connection.read match{
      case Some(data) => {
        handlers.foreach{_(this,data)}
      }
      case None => ()
    }
  }
}
