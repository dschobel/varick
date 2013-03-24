package varick

import java.net.{InetSocketAddress,Socket}

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class EventTests extends FunSpec {

  describe("Varick") {
    it("on connection should fire"){
      val port = 3030
      val server = net.createServer()
      var acceptCount = 0
      server.onAccept((_: Stream) =>  acceptCount += 1)
      server.listen(new InetSocketAddress(port),blocking = false)
      assert(acceptCount === 0)
      new Socket("localhost", port)
      Thread.sleep(100)
      assert(acceptCount === 1)
      new Socket("localhost", port)
      Thread.sleep(100)
      assert(acceptCount === 2)
    }
  }
}
