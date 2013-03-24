package varick

import collection.mutable.ArrayBuffer

trait Event {
  private val events: ArrayBuffer[(String,Function0[Unit])] = ArrayBuffer()
  def on(eventName: String, f: Function0[Unit]) = events += (new Pair(eventName,f))
  def emit(eventName: String) =  events.filter{_._1 == eventName}.foreach{_._2()}
}

trait EventWithArgs{
  private var eventsWithArgs: Vector[(String,Function1[Any,Unit])] = Vector()
  def on[T <: Any](eventName: String, f: Function1[T,Unit]) = eventsWithArgs ++ Vector(new Pair(eventName,f.asInstanceOf[Function1[Any,Unit]]))
  def emit(eventName: String, arg: Any) = {println(eventsWithArgs.size);eventsWithArgs.filter{_._1 == eventName}.foreach{_._2(arg)}}
}
