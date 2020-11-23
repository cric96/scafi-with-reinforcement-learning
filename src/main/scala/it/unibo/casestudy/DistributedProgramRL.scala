package it.unibo.casestudy

import it.unibo.rl.model.QRLImpl
import it.unibo.scafi.learning.{BaseGradientAlgorithm, BaseGradientRL, QLearningProgram, QRLFacade}

/**
 * Distributed Q-learning version in which we imagine that there isn't a central mind that has a clear vision of all the
 * system state.
 */
class DistributedProgramRL extends BaseGradientRL with BaseGradientAlgorithm with QLearningProgram {
  override type ACTION = GAction
  override type STATE = GState
  override type REWARD = Double
  override type FACADE = QRLFacade[STATE, ACTION]
  override def initialSetup: (Double, GAction, GState) = (Double.PositiveInfinity, NoIncrease, NormalState)

  override def algorithm: LearningBasedAlgorithm = new BaseGradientAlgorithm()

  override val learningContext: FACADE = DistributedGQRL.mine(mid())
}
