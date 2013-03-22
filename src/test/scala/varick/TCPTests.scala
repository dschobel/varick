package varick

import java.net.{InetSocketAddress,Socket}

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class TCPTests extends FunSpec with BeforeAndAfter {

  val port = 3030
  var server: Server = _

  before {
    server = net.createServer()
    server.listen(new InetSocketAddress(port),false)
  }

  after{ server.shutdown() }

  describe("Varick") {

    it("should bind to an interface and port and accept connections"){
      val client = new Socket("localhost", port)
        assert(client.isConnected)
    }

    it("accept multiple conections"){
      val client = new Socket("localhost", port)
      val client2 = new Socket("localhost", port)
      assert(client.isConnected)
      assert(client2.isConnected)
    }

    it("should receive tcp requests") (pending)

    it("should respond to tcp requests") (pending)
  }
}

