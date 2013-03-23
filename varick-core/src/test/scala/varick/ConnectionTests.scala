package varick

import java.net.{InetSocketAddress,Socket}
import java.io.PrintWriter

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class ConnectionTests extends FunSpec with BeforeAndAfter {

  val port = 3030
  var server: Server = _

  before {
    server = net.createServer()
    server.listen(new InetSocketAddress(port),false)
  }

  after{ server.shutdown() }

  describe("Varick") {

    it("should bind to an interface and port and accept connections"){
      val socket = new Socket("localhost", port)
      assert(socket.isConnected)
      socket.close
    }

    it("accepts multiple conections"){
      val c1 = new Socket("localhost", port) 
      val c2 = new Socket("localhost", port)
      assert(c1.isConnected)
      assert(c2.isConnected)
      assert(c1.getLocalPort != c2.getLocalPort)
      c1.close
      c2.close
    }

  }
}
