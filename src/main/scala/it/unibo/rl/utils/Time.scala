package it.unibo.rl.utils

object Time {

  /**
   * Facility to track time, just embed the computation in the input
   * @param v
   * @tparam A
   * @return
   */
  def timed[A](v: =>A):A = {
    val t0 = java.lang.System.nanoTime
    try{ v } finally println("Timed op (msec): "+(java.lang.System.nanoTime-t0)/1000000)
  }
}
