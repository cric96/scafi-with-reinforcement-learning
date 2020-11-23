package it.unibo.casestudy

import it.unibo.scafi.learning.{BaseGradientAlgorithm, BaseGradientRL, QRLFacade}

/**
 * We imagine that there is a central mind where we can perform the learning process.
 */
class ConcentratedProgramRL extends BaseGradientRL with BaseGradientAlgorithm {
  override type ACTION = GAction
  override type STATE = GState
  override type REWARD = Double

  override def learningInstance: QRLFacade[GState, GAction] = ConcentrateGQRL

  override def initialSetup: (Double, GAction, GState) = (Double.PositiveInfinity, NoIncrease, NormalState)

  override def algorithm: LearningBasedAlgorithm = new BaseGradientAlgorithm()

  override def learn(algorithm: LearningBasedAlgorithm)(input: => (Boolean, () => Double))(initial: (Double, GAction, GState))(time: Double): Double = {
    val (o0, a0, s0) = initial
    val q = learningInstance
    val (gradient, _, _) = rep((o0, a0, s0)) {
      case (gradientResult, actionT, stateT) =>
        val stateTPlusOne = algorithm.state(gradientResult, stateT, actionT)
        val reward = algorithm.reward(
          output = gradientResult,
          state = stateTPlusOne,
          oldState = stateT,
          action = actionT
        )
        q.qTable = q.qlearning.observeEnvAndUpdateQ(q.qTable, stateTPlusOne, reward, time)
        val actionTPlusOne = q.qlearning.takeEpsGreedyAction(q.qTable, time)
        node.put("state", stateTPlusOne)
        node.put("Qtable", q.qTable)
        node.put("action", actionTPlusOne)
        (algorithm.run(input, actionTPlusOne), actionTPlusOne, stateT)
    }
    gradient
  }

  override def act(algorithm: LearningBasedAlgorithm)(input: => (Boolean, () => Double))(initial: (Double, GAction, GState))(time: Double): Double = {
    val (o0, a0, s0) = initial
    val q = learningInstance
    val (gradient, _, _) = rep((o0, a0, s0)){
      case (gradientResult, actionT, oldState) => val state = algorithm.state(gradientResult, oldState, actionT)
        learningInstance.qlearning.setState(state)
        val action = q.qlearning.takeEpsGreedyAction(q.qTable, time)
        (algorithm.run(input, action), action, state)
    }
    gradient
  }
}
