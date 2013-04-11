package varick

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.Timeouts._
import org.scalatest.concurrent.SocketInterruptor

class BasicTCPTests extends FunSpec with BeforeAndAfter {

  describe("TCPConnection") {
    it("fires event handlers defined on read") {
      val tcp: TCPCodec = new BasicTCP(new StubTCPConnection)
      var count = 0
      val fx: (TCPConnection, Array[Byte]) => Unit ={  (_,_) => println("increment");count += 1 }

      tcp.onRead(fx)
      assert(count === 0)
      tcp.read(List())
      assert(count === 1)
      tcp.read(List())
      assert(count === 2)
    }
  }
}
