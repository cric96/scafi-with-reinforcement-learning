package it.unibo.alchemist.utils

object Clock {
  val initialTime: Long = System.currentTimeMillis()

  def timestampMills() : Long = System.currentTimeMillis() - initialTime

  def timestampSec() : Long = (System.currentTimeMillis() - initialTime) / 1000
}
