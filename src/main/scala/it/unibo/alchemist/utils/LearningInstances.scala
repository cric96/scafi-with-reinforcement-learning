package it.unibo.alchemist.utils

import it.unibo.casestudy.HopCountQRL

import scala.util.Random

object LearningInstances {
  private var qLearningMap = Map.empty[Int, HopCountQRL]
  def isInitialised: Boolean = qLearningMap.nonEmpty
  def initialise(howMany: Int): Unit = if(!isInitialised) {
    println("Initialising..")
    qLearningMap = (0 to howMany).map(id => (id, new HopCountQRL())).toMap
  }
  def mine(id: Int): HopCountQRL = {
    qLearningMap(id)
  }
}
