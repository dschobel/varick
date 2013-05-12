package varick

import java.nio.channels.{SocketChannel, SelectionKey}
import java.nio.ByteBuffer
import java.util.UUID
import scala.util.{Try,Failure,Success}

object TCPConnection{
  val GlobalReadBufferSz: Int = 1024
  val GlobalReadBuffer = ByteBuffer.allocate(GlobalReadBufferSz)
}

/**
 * State and errorhandling for a connected TCP socket
 */
class TCPConnection (val id: UUID, 
                     val key: SelectionKey, 
                         socket: SocketChannel, 
                         initialWriteBufferSz : Int = 1024, 
                         maxWriteBufferSz: Int = 16 * 1024) {

  assert(initialWriteBufferSz > 0)
  assert(maxWriteBufferSz > 0)
  assert(maxWriteBufferSz >= initialWriteBufferSz)

  var writeBuffer: ByteBuffer = ByteBuffer.allocate(initialWriteBufferSz)
  val downStream = collection.mutable.Set[SelectionKey]()
  var upStream:  Option[TCPConnection] = None

  /**
   * close the TCPConnection's underlying socket  
   */
  def close() = {
    socket.close()
    upStream match{
      case Some(conn) => conn.downStream -= key
      case _ => ()
    }
  }

  case class ByteTracker(var bytesSeen: Int, bytesGoal: Int, promiseToFire: scala.concurrent.Promise[Unit])

  private val ByteTrackers = collection.mutable.ArrayBuffer[ByteTracker]()

  def notifyOnBytes(numBytes: Int): scala.concurrent.Future[Unit] = {
    Console println s"monitoring for $numBytes bytes"
    val bt = ByteTracker(0, numBytes, scala.concurrent.Promise[Unit]())
    ByteTrackers += bt
    bt.promiseToFire.future
  }

  /**
   * flowTo make network congestion from sink propagate to this connection
   * @param sink the down-stream TCPConnection
   */
  def flowTo(sink: SocketChannel)(implicit server: TCPServer[_]) = {
    val wrappedChannel = server monitor sink 
    downStream += wrappedChannel.key
    wrappedChannel.upStream = Some(this)
    wrappedChannel
  }

  /** 
   * Reads data from this TCP socket
   */
  def read(): Option[Array[Byte]] = {
    TCPConnection.GlobalReadBuffer.clear()

    Try(socket.read(TCPConnection.GlobalReadBuffer)) match {
      case Failure(e) => {
        println(s"caught ${e.getMessage}, closing socket and cancelling key")
        socket.close() 
        key.cancel()
        None
      }
      case Success(bytesRead: Int) if bytesRead >= 0 => {
        TCPConnection.GlobalReadBuffer.flip()
        val data = TCPConnection.GlobalReadBuffer.array.take(bytesRead)
        Some(data)
      }
      case Success(bytesRead: Int) => {
        socket.close()
        None
      }
    }
  }

  /**
   * indicate whether this conn has any data to write to its client
   */
  def needs_write = writeBuffer.position > 0

  /**
   * write
   * @param data the bytes to write to the socket
   * @return the number of bytes written
   */
  def write(data: Array[Byte]): Int = {
    if(writeBuffer.position() + data.length > maxWriteBufferSz){ throw new java.nio.BufferOverflowException() }
    writeBuffer = ByteBufferUtils.append(writeBuffer,data)
    writeBuffered()
  }

  private def updateByteTrackers(numBytes: Int): Unit = {
    ByteTrackers.foreach{ tracker => tracker.bytesSeen += numBytes }
    val fireable = ByteTrackers.filter{ tracker => tracker.bytesSeen >= tracker.bytesGoal }
    fireable.foreach { tracker =>  tracker.promiseToFire.success(()); ByteTrackers -= tracker }
  }

  /**
    * Non-blocking write for the content of this connections's write buffer
    */
  def writeBuffered(): Int = {
    if(needs_write){
      writeBuffer.flip()
      Try(socket.write(writeBuffer)) match{
        case Failure(e) => {
          println(s"ERROR: caught ${e.getMessage} when trying to write to $id\n\n${e.getStackTrace.take(6).mkString("\n")}")
          socket.close()
          key.cancel()
          -1
        }
        case Success(bytesWritten: Int) => {
          writeBuffer.compact()

          //if a socket needs to write more data, register interest with selector
          val writeInterest = if (needs_write) SelectionKey.OP_WRITE else ~SelectionKey.OP_WRITE
          key.interestOps(key.interestOps() & writeInterest)
          updateByteTrackers(bytesWritten)
          bytesWritten
        }
      }
    }
    else{
      println("WARN: write buffer is empty. Calling writeBuffered with empty buffers is probably not what you intended")
      0
    }
  }
}
