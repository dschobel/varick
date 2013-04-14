package varick

import collection.mutable.ArrayBuffer


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

  type ProtocolData = Array[Byte] //WrappedByteArray

  //implicit def byteArrayWrapper(bytes: Array[Byte]): WrappedByteArray = new WrappedByteArray(bytes)

  override def read(handlers: Seq[Function2[TCPCodec, Array[Byte],Unit]]) = {
    connection.read match{
      case Some(data) => {
        handlers.foreach{_(this,data)}
      }
      case None => ()
    }
  }
}
