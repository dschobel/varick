package varick

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter

class BasicTCPTests extends FunSpec with BeforeAndAfter {

  describe("TCPConnection") {
    it("fires event handlers defined on read") {
      val tcp: TCPCodec = new BasicTCP(new StubTCPConnection)
      var count = 0

      tcp.onRead((_,_) => {println("increment");count += 1})
      assert(count === 0)
      tcp.read(List())
      assert(count === 1)
      tcp.read(List())
      assert(count === 2)
    }
  }
}
