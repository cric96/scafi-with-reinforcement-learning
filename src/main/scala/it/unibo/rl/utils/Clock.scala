package it.unibo.rl.utils

object Clock {
  val initialTime = System.currentTimeMillis()

  def timestampMills() : Long = System.currentTimeMillis() - initialTime

  def timestampSec() : Long = (System.currentTimeMillis() - initialTime) / 1000

}
