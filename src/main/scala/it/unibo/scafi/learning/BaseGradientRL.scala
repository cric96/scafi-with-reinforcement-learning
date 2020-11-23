package it.unibo.scafi.learning

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.casestudy._
import it.unibo.rl.utils.Clock

/**
 * case study: we want to improve the gradient convergence. The idea here is, when a node perceived a gradient increase, the node
 * enforces the gradient value with a constant scalar value (e.g. 5.0). This might bring to move the gradient faster to the true value.
 * This is a very simplistic scenario:
 * - the state space is: { NormaleState, RisingSlowlyState }
 * - the action space is : { NoIncrease, IncreaseLittle, IncreaseMuch }
 * we expected to reach this deterministic policy:
 *  pi(NormalState) -> NoIncrease
 *  pi(RisingSlowlyState) -> IncreaseMuch
 * here, to eval the state, we use a temporal window to see if the gradient is increasing (for example, in the last three round).
 * We verify this algorithm using a central mind based on Q-Learning and a distributed version using a independent learning.
 * --- ALCHEMIST CONFIGURATION ---
 * in the yaml file, you must define what is the initial gradient source (a molecule called right_source) and the other gradient source
 * (a molecule called left_source). Furthermore, you must specified when the gradient source must change (a molecule called stop_right_source)
 */
abstract class BaseGradientRL extends AggregateProgram with StandardSensors with Gradients with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with RLGradientLib with AggregateProgramWithLearning {
  override type OUTPUT = Double
  override type INPUT = (Boolean, () => Double)

  /**
   * TEMPLATE METHOD. defined what learning algorithm should be used in the learning process.
   */
  def algorithm : LearningBasedAlgorithm

  /**
   * define the initial state of each node (it might be create randomly in Monte Carlo setup)
   */
  def initialSetup : (OUTPUT, ACTION, STATE)

  /**
   * define what type of q-learning techniques is used in this process
   */
  def learningInstance : QRLFacade[STATE, ACTION]
  def intMoleculeOf(name : String) : Int = Integer.parseInt((node.get[Any](name)).toString)
  //Variable loaded by alchemist configuration.
  lazy val LEFT_SRC = intMoleculeOf("left_source")       // ID of the source at the left of the env (the stable one)
  lazy val RIGHT_SRC = intMoleculeOf("right_source")      // ID of the source at the right of the env (the unstable one)
  lazy val RIGHT_SRC_STOP: Int = intMoleculeOf("stop_right_source") // time at which the source at the right of the env stops being a source

  def range = node.get[Double]("range")

  //workaround, the ref gradient is a var because it could be used to evaluate the reward. try to find a better solution
  var refGradient : Double = 0.0
  override def main(): Any = {
    // SIMULATION DYNAMICS, BASELINE, and REFERENCE, IDEAL GRADIENT FOR COMPARISON AND ERROR CALCULATION
    val passedTime = timestamp()
    val source = if (mid == LEFT_SRC || (mid == RIGHT_SRC && passedTime < RIGHT_SRC_STOP)) true else false
    node.put("src", source)
    val classicG = classicGradient(source) // BASELINE
    val gWithoutRightSource = classicGradient(mid == LEFT_SRC) // optimal gradient when RIGHT_SRC stops being a source
    refGradient = if(passedTime>=RIGHT_SRC_STOP) gWithoutRightSource else classicG

    // RL-BASED GRADIENT: THE OBJECT OF OUR EXPERIMENTS
    val rlbasedG : OUTPUT = learn(algorithm)((source, () => nbrRange()))(initialSetup)(Clock.timestampSec()) //todo fix

    // EXPORTS
    node.put("classicG", classicG)
    node.put("refG", refGradient)
    node.put("rlbasedG", rlbasedG)
    node.put(s"err_classicG", Math.abs(refGradient - classicG))
    node.put(s"err_rlbasedG", Math.abs(refGradient - rlbasedG))
    node.put(s"passed_time", passedTime)
  }
}

/**
 * a standard target version of our learning gradient algorithm. It increase the output gradient by the action increment
 * specified.
 */
trait BaseGradientAlgorithm {
  self : BaseGradientRL =>

  override type ACTION = GAction
  override type STATE = GState
  override type REWARD = Double

  /**
   * our standard gradient learning algorithm
   * @param temporalWindow how many export I should consider to change the state in RisingSlowly?
   * @param increasingThreshold if i sense an increasing values, what is the tolerance to bring me in a RisingSlowly setting?
   */
  class BaseGradientAlgorithm(temporalWindow : Int = 3, increasingThreshold : => Double = range/10) extends LearningBasedAlgorithm {
    override final def run(input: (Boolean, () => Double), action: ACTION): Double = {
      val (source, metric) = input
      rep(Double.PositiveInfinity)(d =>
        mux(source) {
          0.0
        } {
          minHoodPlus(nbr(d) + metric()) + action.increment
        }
      )
    }

    /* the state here is NormalState if the gradient is stable for 'temporalWindows' time or the increasing delta value is under a certain increasingThreshold */
    def state(output: Double, oldState: STATE, action: ACTION): STATE = {
      val myVariability = varianceFor(temporalWindow, output)
      val variability = meanHood(nbr(myVariability))
      node.put("myvar", myVariability)
      node.put("nbrvar", variability)

      val rvs = recentValues(temporalWindow, output)
      node.put("debug_recentVals", rvs)
      node.put("debug_isIncreasing", isIncreasing(output, temporalWindow))
      node.put("debug_delta", delta(output, temporalWindow))
      node.put("debug_isStable", isStable(output, temporalWindow))
      node.put("debug_myvariance", myVariability)

      //& delta(o, k=3)<range/10 ?? what is 10?? to
      val state : STATE = if(isIncreasing(output, temporalWindow) & delta(output, temporalWindow) < increasingThreshold) RisingSlowlyState else NormalState
      node.put("debug_state", recentValues(temporalWindow, state))
      state
    }
    /* a 'human friendly' reward function, we give a very high reward if we consider (oldState, action) good, a negative reward otherwise */
    override def reward(output: OUTPUT, state: STATE, oldState: STATE, action: ACTION): REWARD = {
      val reward = (output, state, action) match {
        case _ if isStable(output, temporalWindow) => 1
        case (_, RisingSlowlyState, IncreaseMuch) => 50
        case (_, RisingSlowlyState, NoIncrease) => -5
        case (_, NormalState, NoIncrease) => 5
        case (_, NormalState, IncreaseMuch) => -2
        case _ =>  -2
      }
      node.put("debug_reward", recentValues(k=10, reward))
      reward
    }
  }
}