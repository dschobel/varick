package varick

import java.net.{InetSocketAddress,Socket}
import java.io.PrintWriter

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.time.SpanSugar._
import org.scalatest.concurrent.Timeouts._
import org.scalatest.concurrent.SocketInterruptor


class EchoTests extends FunSpec with BeforeAndAfter {

  describe("Varick") {

    it("can send and receive data from clients") {
      val port = 3030
      val echo = net.createServer()

      echo.onRead{(tcp, data) => tcp.connection.write(data.toBytes()) }
      echo.listen(new InetSocketAddress(port),blocking = false)

      val socket = new Socket("localhost", port)
      val out = new PrintWriter(socket.getOutputStream(), true)
      val in = socket.getInputStream()
      println("client: sending data")
      val message = "hello from the client!"
      out.println(message)
      println("client: data sent")
      val readBuffer: Array[Byte] = Array.fill(message.length){0}
      var response: Int = 0
      println("client: waiting for data")
      implicit val killit = new SocketInterruptor(socket)
      failAfter(2 seconds) {
       response = in.read(readBuffer,0,readBuffer.length)
      }
      println(s"finished readLine, result is: ${new String(readBuffer.take(response))}")
      assert(message.length === response)
      echo.shutdown()
      socket.close()
    }
  }
}
