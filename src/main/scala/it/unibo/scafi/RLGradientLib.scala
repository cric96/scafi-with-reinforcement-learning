package it.unibo.scafi

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.ScafiAlchemistSupport

import scala.collection.immutable.Queue
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
trait RLGradientLib { self: AggregateProgram with FieldUtils with ScafiAlchemistSupport =>

  // TODO: add in ScaFi stdlib
  def delta(value: Double, k: Int = 2, default: Double = Double.PositiveInfinity): Double = {
    val vs = recentValues(k, value)
    if(vs.drop(k-1).isEmpty) default else vs.last-vs.head
  }

  // TODO: add in ScaFi stdlib
  def deltas[T:Numeric](value: T, k: Int = 2, default: Boolean = false): Iterable[T] = {
    val vs = recentValues(k, value)
    if(vs.drop(1).isEmpty)
      List()
    else
      vs.sliding(size=2, step=1)
        .map(q => implicitly[Numeric[T]].minus(q.head, q.drop(1).head)).toList
  }

  // TODO: add in ScaFi stdlib
  def isIncreasing[T : Ordering](value: T, k: Int = 2, default: Boolean = false): Boolean = {
    val vs = recentValues(k, value)
    if(vs.drop(1).isEmpty)
      default
    else {
      vs.sliding(size = 2, step = 1)
        .map(q => implicitly[Ordering[T]].compare(q.head, q.drop(1).head))
        .sum < 0
    }

  }

  // TODO: add in ScaFi stdlib
  def isStable[T](value: T, k: Int = 2): Boolean = {
    val r = recentValues(k, value).forall(_ == value)
    //println(s"value ${r}")
    r
  }
  // TODO: add in ScaFi stdlib
  def previous[T](value : T): Option[T] =
    recentValues(2, value).dropRight(1).headOption

  // TODO: add in ScaFi stdlib
  def meanHood(v: Double): Double =
    excludingSelf.sumHood(nbr(v)) / excludingSelf.sumHood(1)

  // TODO: add in ScaFi stdlib
  def varianceFor(k: Int, value: Double): Double = {
    val vs = recentValues(k, value)
    val len = vs.length
    val mean = vs.sum / len
    vs.foldLeft(0.0)((acc,v) => acc+Math.pow(v-mean, 2)) / len
  }

  // TODO: add in ScaFi stdlib
  def recentValues[T](k: Int, value: T): Queue[T] =
    rep((Queue[T]())){ case (vls) => {
      (if(vls.size==k) vls.tail else vls) :+ value
    }}
}
