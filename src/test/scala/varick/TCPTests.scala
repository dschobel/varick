package varick

import java.net.{InetSocketAddress,Socket}
import org.scalatest.FunSpec


class TCPTests extends FunSpec {

  describe("Varick") {

    it("should create a new server instance when calling createServer"){
      val server = net.createServer()
    }

    it("should bind to an interface and port"){
      val server = net.createServer()
      val port = 3030
      server.listen(new InetSocketAddress(port),false)

      val client = new Socket("localhost", port)
    }

    it("should receive tcp requests") (pending)

    it("should respond to tcp requests") (pending)
  }
}

