package varick

import collection.mutable.ArrayBuffer

trait Event{
  import collection.mutable.ArrayBuffer

  private val eventHandlers: ArrayBuffer[Pair[String,Function0[Unit]]] = ArrayBuffer()
  private val eventHandlersWithArgs : ArrayBuffer[Pair[String,Function1[Any,Unit]]] = ArrayBuffer()

  def on(eventName: String, action: Function0[Unit]) = eventHandlers += new Pair(eventName,action)
  def on[T](eventName: String, action: Function1[T,Unit]) = eventHandlersWithArgs += new Pair(eventName,action.asInstanceOf[Function1[Any,Unit]])

  def emit(eventName: String) = eventHandlers.filter(_._1 == eventName).foreach{_._2()}
  def emit[T](eventName: String, arg: T) = eventHandlersWithArgs.filter(_._1 == eventName).foreach{_._2(arg)}
}
