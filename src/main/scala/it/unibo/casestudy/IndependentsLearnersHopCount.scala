package it.unibo.casestudy

import it.unibo.alchemist.utils.LearningInstances
import it.unibo.scafi.learning.{BaseHopCountAlgorithm, BaseHopCountRL}

/**
 * Distributed Q-learning version in which we imagine that there isn't a central mind that has a clear vision of all the
 * system state.
 */
class IndependentsLearnersHopCount extends BaseHopCountRL with BaseHopCountAlgorithm {

  override lazy val learningInstance = LearningInstances.mine(mid)
  implicit lazy val rand = randomGen

  override def initialSetup = (HopCountQRL.inf, 0, (HopCountQRL.inf, 0))

  override def algorithm: LearningBasedAlgorithm = new BaseHopCountAlgorithm()

  override def learn(algorithm: LearningBasedAlgorithm)(input: => (Boolean, () => Int))(initial: (Int, Int, (Int, Int)))(time: Double): Int = {
    val (o0, a0, s0) = initial
    val q = learningInstance
    val (hopCount, _, _) = rep((o0, a0, s0)) {
      case (hopCountResult, actionT, stateT) =>
        //println(s"o: $o, a: $a")
        q.qlearning.setState(stateT)
        val stateTPlusOne = algorithm.state(hopCountResult, stateT, actionT)
        //println(s"qtable first: ${q.qTable}")
        val reward = algorithm.reward(
          output = hopCountResult,
          state = stateTPlusOne,
          oldState = stateT,
          action = actionT
        )
        q.qTable = q.qlearning.observeEnvAndUpdateQ(q.qTable, stateTPlusOne, reward, time)
        val actionTPlusOne = q.qlearning.takeEpsGreedyAction(q.qTable, time)
        node.put("state", stateTPlusOne)
        node.put("action", actionTPlusOne)
        (algorithm.run(input, actionTPlusOne), actionTPlusOne, stateTPlusOne)
    }
    hopCount
  }

  override def act(algorithm: LearningBasedAlgorithm)(input: => (Boolean, () => Int))(initial: (Int, Int, (Int, Int)))(time: Double): Int = {
    val (o0, a0, s0) = initial
    val q = learningInstance//IndependentHopCountRL.average
    val (hopCount, _, _) = rep((o0, a0, s0)){
      case (hopCountResult, actionT, oldState) => val state = algorithm.state(hopCountResult, oldState, actionT)
        q.qlearning.setState(state)
        val action = q.qlearning.takeGreedyAction(q.qTable)
        (algorithm.run(input, action), action, state)
    }
    hopCount
  }
}
