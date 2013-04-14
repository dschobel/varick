package varick

import collection.mutable.ArrayBuffer


object TCPBuilder extends ProtocolBuilder[BasicTCP]{
  override def build(conn: TCPConnection) = new BasicTCP(conn)
}

/**
  * a thin wrapper class which passes all reads and writes directly to the underlying TCP transport without any en/decoding
  */
class BasicTCP(c: TCPConnection) extends TCPCodec[Array[Byte]](c){

  //type ProtocolData = Array[Byte]

  override def read(handlers: Seq[Function2[TCPCodec[Array[Byte]], Array[Byte],Unit]]) = {
    connection.read match{
      case Some(data) => {
        handlers.foreach{_(this,data)}
      }
      case None => ()
    }
  }
}
