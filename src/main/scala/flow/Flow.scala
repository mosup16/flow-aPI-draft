package flow

import scala.annotation.tailrec

trait Flow[+T] {

  def getNext: Option[T]

  def flatmap[U](f: T => Flow[U]): Flow[U]

  protected def flatten[U](flow: Flow[Flow[U]]): Flow[U]

  def map[U](f: T => U): Flow[U]

  def filter(f: T => Boolean): Flow[T]

  def forEach(f: T => Unit): Unit

}

object Flow {
  def of[T](iterable: Iterable[T]): Flow[T] = {
    val it = iterable.iterator
    new ProducerFlow[T](() => if (it.hasNext) Some(it.next) else None)
  }
}

abstract class AbstractFlow[+T] extends Flow[T] {

  protected def flatten[U](flow: Flow[Flow[U]]): Flow[U] = new FlattenedFlow[U](() => flow.getNext)

  override def flatmap[U](f: T => Flow[U]): Flow[U] = flatten(map(f))

  override def map[U](f: T => U): Flow[U] = new ProducerFlow[U](() => this.getNext.map(f))

  override def filter(f: T => Boolean): Flow[T] =
    new ProducerFlow[T](() => {
      @tailrec
      def go(e: Option[T]): Option[T] =
        if (e.isEmpty) e // to indicate the end of the flow
        else if (f(e.get)) e else go(this.getNext)

      go(getNext)
    })

  override def forEach(f: T => Unit): Unit = {
    @tailrec
    def go(e: Option[T]): Unit =
      if (e.isEmpty) () else { // termination condition
        f(e.get)
        go(getNext)
      }

    go(getNext)
  }
}

case class ProducerFlow[+T](producer: () => Option[T]) extends AbstractFlow[T] {
  override def getNext: Option[T] = {
    val v = producer()
    println("getNext -> ProducerFlow i :> " + v)
    v
  }

  override def toString: String = "ProducerFlow "
}

class FlattenedFlow[T](producer1: () => Option[Flow[T]])
  extends AbstractFlow[T] {
  var moveToNextFlow: Boolean = true
  var currentFlow: Option[Flow[T]] = getCurrentFlow

  /*
  * first checks to see if the flow is terminated
  * if so we should then move on to the next one
  * or just return the current one and should inform you that there is no flow is left
  */
  def getCurrentFlow: Option[Flow[T]] = {
    if (moveToNextFlow) {
      val nextFlow = getNextFlow
      moveToNextFlow = false
      currentFlow = nextFlow match {
        case None => nextFlow // indicates the termination of the flow
        case Some(_) => nextFlow
      }
    }
    currentFlow
  }

  def getNextFlow: Option[Flow[T]] = producer1()

  override def getNext: Option[T] = {
    @tailrec
    def go(e: Option[T]): Option[T] = e match {
      case Some(t) => e
      case None => // current flow is terminated so move to the next one
        moveToNextFlow = true // indicates that we will move to the next flow
        val f = getCurrentFlow
        f match {
          case Some(flow) => go(flow.getNext)
          case None => None // indicates the termination of the flow
        }
    }

    val f = getCurrentFlow
    val v = f match {
      case Some(v) => go(v.getNext)
      case None => None // indicates the termination of the flow
    }
    println("getNext -> FlattenedFlow i :> " + v)
    v
    //equivalent to getCurrentFlow().map(v => v.getNext)
  }
  override def toString: String = "FlattenedFlow "

}

