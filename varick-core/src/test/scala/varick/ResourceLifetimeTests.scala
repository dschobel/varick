package varick

import java.net.{InetSocketAddress,Socket}

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class LifetimeTests extends FunSpec {

  describe("Varick") {
    it("it should close the server socket on shutdown"){
      val port = 3030
      val server = net.createServer()
      server.listen(new InetSocketAddress(port),blocking =false)
      assert(false == server.socket.isClosed)
      server.shutdown 
      assert(server.socket.isClosed)
    }
    it("it should disconnect a connected client on shutdown"){
      val port = 3030
      val server = net.createServer()
      server.listen(new InetSocketAddress(port),blocking = false)
      val conn = new Socket("localhost", port)
      assert(conn.isConnected)
      server.shutdown 
      //TODO: assert(false === conn.isConnected)
    }
  }
}
