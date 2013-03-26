package varick

import java.net.{InetSocketAddress,Socket}
import java.io.{PrintWriter,InputStreamReader}

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class EventTests extends FunSpec {

  describe("Varick") {
    it("on connection should fire once for every new client socket"){
      val port = 3030
      val server = net.createServer()
      var acceptCount = 0
      server.onAccept((_: Stream) =>  acceptCount += 1)
      server.listen(new InetSocketAddress(port),blocking = false)
      assert(acceptCount === 0)
      println("client 1 connecting...")
      new Socket("localhost", port)
      Thread.sleep(50) //the joys of testing server events 
                         //from the client side...
      assert(acceptCount === 1)
      new Socket("localhost", port)
      println("client 2 connecting...")
      Thread.sleep(50)
      assert(acceptCount === 2)
      server.shutdown()
    }

    it("data event should fire for every partial socket read "){
      val port = 3030
      val server = net.createServer()
      var readCount = 0
      server.onAccept((stream: Stream) =>  {
          stream.onData( (_: Array[Byte]) => { readCount += 1 })
        })
      server.listen(new InetSocketAddress(port),blocking = false)
      assert(readCount === 0)
      val client = new Socket("localhost", port)
      val out = new PrintWriter(client.getOutputStream(), true)
      out.println("hello")
      Thread.sleep(50)
      assert(readCount === 1)
      server.shutdown()
    }
  }
}
