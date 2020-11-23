package it.unibo.scafi.learning
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._

trait QLearningProgram extends AggregateProgramWithLearning {
  self: AggregateProgram =>
  type FACADE = QRLFacade[STATE, ACTION]
  override type REWARD = Double
  val learningContext : FACADE

  def learn(algorithm: LearningBasedAlgorithm)(input: => INPUT)(initial: (OUTPUT, ACTION, STATE))(time: Double): OUTPUT = {
    val (o0, a0, s0) = initial
    val qLearning = learningContext.qlearning
    val (programOutput, _, _) = rep((o0, a0, s0)) {
      case (aggregateResult, actionT, stateT) =>
        //println(s"o: $o, a: $a")
        qLearning.setState(stateT)
        val stateTPlusOne = algorithm.state(aggregateResult, stateT, actionT)
        //println(s"qtable first: ${q.qTable}")
        val reward = algorithm.reward(
          output = aggregateResult,
          state = stateTPlusOne,
          oldState = stateT,
          action = actionT
        )
        qLearning.observeEnvAndUpdateQ(qLearning.q0, stateTPlusOne, reward, time)
        val actionTPlusOne = qLearning.takeEpsGreedyAction(qLearning.q0, time)
        (algorithm.run(input, actionTPlusOne), actionTPlusOne, stateTPlusOne)
    }
    programOutput
  }
  def act(algorithm: LearningBasedAlgorithm)(input: => INPUT)(initial: (OUTPUT, ACTION, STATE))(time: Double): OUTPUT = {
    val (o0, a0, s0) = initial
    val qLearning = learningContext.qlearning
    val (gradient, _, _) = rep((o0, a0, s0)){
      case (aggregateResult, actionT, oldState) => val state = algorithm.state(aggregateResult, oldState, actionT)
        qLearning.setState(oldState)
        val action = qLearning.takeEpsGreedyAction(qLearning.q0, time)
        qLearning.setState(state)
        (algorithm.run(input, action), action, state)
    }
    gradient
  }
}
