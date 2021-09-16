package it.unibo.casestudy

import it.unibo.rl.model.QRLImpl
import it.unibo.scafi.learning.QRLFacade

import scala.language.postfixOps
import scala.util.Random

class HopCountQRL(randomGen: Random)
  extends QRLFacade[(Int, Int), Int](new QRLImpl[(Int, Int), Int]{
    override implicit val random: Random = randomGen
  }, HopCountQRL.actions) with Serializable {
  val states : Set[(Int, Int)] = for {
    state <- HopCountQRL.states
    action <- HopCountQRL.actions
  } yield (state, action)
}

object HopCountQRL {
  val inf = 10000
  val maxState = 6
  val actions: Set[Int] = 0 to 2 toSet
  val stateRange: Set[Int] = 0 to maxState toSet
  val states: Set[Int] = stateRange.map(_* -1) ++ stateRange + inf + (-inf)
}
