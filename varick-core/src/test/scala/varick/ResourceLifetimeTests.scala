package varick

import java.net.{InetSocketAddress,Socket}

import org.scalatest.FunSpec


class ResourceLifetimeTests extends FunSpec {

  describe("Varick") {
    it("it should close the server socket on shutdown"){
      val port = 3030
      val server = net.createServer()
      server.listen(new InetSocketAddress(port),blocking =false)
      assert(false == server.socket.isClosed)
      server.shutdown()
      assert(server.socket.isClosed)
    }

    it("should stop accepting new connections on shutdown"){
      val port = 3030
      val server = net.createServer()
      server.listen(new InetSocketAddress(port), blocking = false)
      val client = new Socket("localhost", port)
      assert(client.isConnected) //first client connects

      server.shutdown()
      val thrown = intercept[java.net.ConnectException] { new Socket("localhost", port) }
      assert(thrown.getMessage === "Connection refused")
    }
  }
}
