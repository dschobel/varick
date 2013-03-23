package varick

import java.net.{InetSocketAddress,Socket}

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class LifetimeTests extends FunSpec {

  describe("Varick") {
    it("it should unbind from on shutdown"){
      val port = 3030
      val server = net.createServer()
      server.listen(new InetSocketAddress(port),false, true)
      val conn = new Socket("localhost", port)
      assert(conn.isConnected)
      server.shutdown
      assert(false === conn.isConnected)
    }
  }
}
