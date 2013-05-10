package varick


trait ProducerConsumerFactory[T]{
  def buildProducer(): Producer[T]
  def buildConsumer(): Consumer[T]
}

trait Producer[T]{
  import scala.util.Try

  def next(): Try[T]
  def pause(): Unit
  def resume(): Unit
  def paused: Boolean
}

trait Consumer[T]{
  def hasCapacity: Boolean
  def consume(data: T)
}
