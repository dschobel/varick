package varick

import java.net.{InetSocketAddress,Socket}
import java.io.{PrintWriter,BufferedReader,InputStreamReader}

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter


class EchoTests extends FunSpec with BeforeAndAfter {

  describe("Varick") {
    it("can send and receive data from clients") {
      val port = 3030
      val server = net.createServer()
      //server.on(:connection) do |connection|
        //connection.on(:data) do |data|
          //connection.write(data)
        //end
      //end
       // conn.onData((data: Array[Byte]) => conn.write(_)))

      server.listen(new InetSocketAddress(port),blocking = false)
      val socket = new Socket("localhost", port)

      server.shutdown
      //val out = new PrintWriter(socket.getOutputStream(), true)
      //val in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
      println("sending data")
      //val out = new PrintWriter(socket.getOutputStream(), true)
      //out.println("hello world")
      println("data sent")
      println("waiting for data")
      //val line = in.readLine()
      //println(s"finished readLine, result is: $line")

      //socket.close
      //server.shutdown()
    }
  }
}
