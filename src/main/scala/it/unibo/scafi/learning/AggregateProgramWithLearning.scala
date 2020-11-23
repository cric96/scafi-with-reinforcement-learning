package it.unibo.scafi.learning
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
/**
 * A root interface used to define an aggregate program that use some learning mechanisms. It is based on MDP, each agent
 * need to defined the ACTION, STATE, REWARD type and the INPUT and OUTPUT type of an aggregate program.
 * We suppose that each agent doesn't know the environment model, but should sense its state in some ways.
 */
trait AggregateProgramWithLearning {
  self: AggregateProgram =>
  type ACTION // action space of learning algorithm
  type STATE // state space of learning algorithm
  type REWARD // in general we suppose that the reward is a scalar, but to make this concept more general we don't bound it
  type INPUT // the AC program input
  type OUTPUT // the AC program output

  /**
   * This interface define the aggregate algorithm. The run could contain the entire aggregate program logic on only a little part.
   */
  trait LearningBasedAlgorithm {
    /**
     * The application logic associated to the aggregate program. It uses the action selected by the learning algorithm.
     *
     * @param input  input used by the aggregate program to perform some application logic
     * @param action the action that could enhance the aggregate program in some way
     * @return the output produce by the aggregate program
     */
    def run(input: INPUT, action: ACTION): OUTPUT

    /**
     * Perceive current environmental state.
     * idea: giving a time T, what is the  ST+1 based on ST and AT?
     *
     * @param output   data produce by the aggregate program that should be used to create the agent state
     * @param oldState the old state perceived by the agent
     * @param action   the action selected performed on old state
     * @return the new state perceived by the agent
     */
    def state(output: OUTPUT, oldState: STATE, action: ACTION): STATE

    /**
     * Eval the instantaneous reward to give to the computational node
     *
     * @param output   current aggregate program output
     * @param state    current perceived node state (time T+1)
     * @param oldState old perceived node state (time T)
     * @param action   action choosed that bring the node from state 'oldState' to 'state' (time T)
     * @return a reward that describe the goodness of the action choosed
     */
    def reward(output: OUTPUT, state: STATE, oldState: STATE, action: ACTION): REWARD
  }

  /**
   * Giving the learning algorithm, the node try to optimize the long the reward signal, using some algorithm (e.g. Monte Carlo ES, Q-Learning, SARSA,...)
   *
   * @param algorithm the reference application logic to optimize
   * @param input     the input of algorithm
   * @param initial   initial configuration of the algorithm
   * @param time      express the time evolution
   * @return the algorithm output
   */
  def learn(algorithm: LearningBasedAlgorithm)(input: => INPUT)(initial: (OUTPUT, ACTION, STATE))(time: Double): OUTPUT

  /**
   * Giving the learning algorithm, he use the knowledge learned to act upon it. Use act when the learning process is over and you
   * want to use the experience learned (i.e. if you use Generalised Policy Iteration, when you reach the optimal policy, you can stop
   * to improve it)
   *
   * @param algorithm the reference application logic to optimize
   * @param input     the input of algorithm
   * @param initial   initial configuration of the algorithm
   * @param time      express the time evolution
   * @return the algorithm output
   */
  def act(algorithm: LearningBasedAlgorithm)(input: => INPUT)(initial: (OUTPUT, ACTION, STATE))(time: Double): OUTPUT
}
