package it.unibo.casestudy

import it.unibo.scafi.learning.{BaseGradientAlgorithm, BaseGradientRL, QLearningCentralizedProgram, QRLFacade}

/**
 * We imagine that there is a central mind where we can perform the learning process.
 */
class ConcentratedProgramRL extends BaseGradientRL with BaseGradientAlgorithm with QLearningCentralizedProgram {
  override type ACTION = GAction
  override type STATE = GState
  override type REWARD = Double
  override type FACADE = QRLFacade[GState, GAction]
  /**
   * TEMPLATE METHOD. defined what learning algorithm should be used in the learning process.
   */
  override def algorithm: LearningBasedAlgorithm = new BaseGradientAlgorithm()

  /**
   * define the initial state of each node (it might be create randomly in Monte Carlo setup)
   */
  override def initialSetup: (Double, GAction, GState) = (Double.PositiveInfinity, NoIncrease, NormalState)

  override val learningContext: FACADE = ConcentrateGQRL
}
