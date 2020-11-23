package it.unibo.casestudy

import it.unibo.rl.model.QRLImpl
import it.unibo.scafi.learning.QRLFacade

/**
 * the case study state space, it is defined as ADT
 */
trait GState

/**
 * the agent perceives no gradient increasing
 */
case object NormalState extends GState

/**
 * the agent perceives a rising value
 */
case object RisingSlowlyState extends GState

/**
 * the case study action space, it is defined as ADT
 */
trait GAction { def increment: Double }

/**
 * increase the value of gradient by a increment double value
 * @param increment the amount of increment
 */
abstract class Increase(val increment: Double = 0.0) extends GAction

/**
 * increase the value of a increment bigger then 'IncreaseLittle'
 */
case object IncreaseMuch extends Increase(5.0)

/**
 * increase the value of a increment smaller then 'IncreaseMuch'
 */
case object IncreaseLittle extends Increase(1.0)

/**
 * retain the gradient value without adding deltas.
 */
case object NoIncrease extends Increase(0.0)


class GQRL extends QRLFacade[GState, GAction](
  new QRLImpl[GState,GAction]{ },
  Set[GAction](NoIncrease, IncreaseLittle, IncreaseMuch)
) {
  val states = Set[GState](NormalState, RisingSlowlyState)
}

/**
 * a common reference to the GQRL learning instance, used to perform learning as a whole.
 */
object ConcentrateGQRL extends GQRL