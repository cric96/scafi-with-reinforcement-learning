package it.unibo.casestudy

import it.unibo.rl.model.QRLImpl
import it.unibo.scafi.learning.QRLFacade

import scala.language.postfixOps

class HopCountQRL()
  extends QRLFacade[List[Int], Int](new QRLImpl[List[Int], Int]{
  }, HopCountQRL.actions) with Serializable {
  val states : List[Int] = for {
    state <- HopCountQRL.states.toList
  } yield (state)
}

object HopCountQRL {
  val inf = 100000
  val maxState = 1
  val actions: Set[Int] = 0 to 1 toSet
  val stateRange: Set[Int] = 0 to maxState toSet
  val states: Set[Int] = stateRange.map(_* -1) ++ stateRange + inf + (-inf)
}
