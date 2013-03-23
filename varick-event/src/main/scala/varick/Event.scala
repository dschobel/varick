package varick

import collection.mutable.ArrayBuffer

trait Event{

  private val events: ArrayBuffer[(String,Function0[Unit])] = ArrayBuffer()

  def on(eventName: String, f: Function0[Unit]): Unit = events += (new Pair(eventName,f))

  def emit(eventName: String): Unit = events.filter{_._1 == eventName}.foreach{_._2()}
  
}
