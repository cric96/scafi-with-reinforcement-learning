package it.unibo.scafi.learning

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.ScafiAlchemistSupport
import it.unibo.casestudy.{GAction, GState, HopCountQRL, IncreaseMuch, NoIncrease, NormalState, RLGradientLib, RisingSlowlyState}
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.rl.utils.Clock


abstract class BaseHopCountRL extends AggregateProgram with StandardSensors with Gradients with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with RLGradientLib with AggregateProgramWithLearning{

  override type OUTPUT = Int
  override type INPUT = (Boolean, () => Int)

  def algorithm : LearningBasedAlgorithm

  def initialSetup : (OUTPUT, ACTION, STATE)
  def learningInstance : QRLFacade[STATE, ACTION]
  def intMoleculeOf(name : String) : Int = Integer.parseInt((node.get[Any](name)).toString)
  //Variable loaded by alchemist configuration.
  lazy val LEFT_SRC = intMoleculeOf("left_source")       // ID of the source at the left of the env (the stable one)
  lazy val RIGHT_SRC = intMoleculeOf("right_source")      // ID of the source at the right of the env (the unstable one)
  lazy val RIGHT_SRC_STOP: Int = intMoleculeOf("stop_right_source") // time at which the source at the right of the env stops being a source
  private val hopCountMetric : Metric = () => 1.0
  override def main(): Any = {
    // SIMULATION DYNAMICS, BASELINE, and REFERENCE, IDEAL GRADIENT FOR COMPARISON AND ERROR CALCULATION
    val passedTime = timestamp()
    val source = if (mid == LEFT_SRC || (mid == RIGHT_SRC && passedTime < RIGHT_SRC_STOP)) true else false
    node.put("src", source)
    val classicHopCount = classicGradient(source, hopCountMetric) // BASELINE
    val hopCountWithoutRightSource = classicGradient(mid == LEFT_SRC, hopCountMetric) // optimal gradient when RIGHT_SRC stops being a source
    val refHopCount = if(passedTime>=RIGHT_SRC_STOP) hopCountWithoutRightSource else classicHopCount
    // Can be used in the learning session
    node.put("refHopCount", refHopCount)

    // RL-BASED GRADIENT: THE OBJECT OF OUR EXPERIMENTS
    val rlbasedHopCount : OUTPUT = if(Clock.timestampSec() < 100) {
      learn(algorithm)((source, () => hopCountMetric().toInt))(initialSetup)(Clock.timestampSec())
    } else {
      act(algorithm)((source, () => hopCountMetric().toInt))(initialSetup)(Clock.timestampSec())
    }//todo fix
    // EXPORTS
    node.put("classicHopCount", classicHopCount)
    node.put("rlbasedHopCount", rlbasedHopCount)
    node.put(s"err_classicHopCount", Math.abs(refHopCount - classicHopCount))
    node.put(s"err_rlbasedHopCount", Math.abs(refHopCount - rlbasedHopCount))
    node.put(s"passed_time", passedTime)
  }
}

/**
 * a standard target version of our learning gradient algorithm. It increase the output gradient by the action increment
 * specified.
 */
trait BaseHopCountAlgorithm {
  self : BaseHopCountRL =>

  override type ACTION = Int
  override type STATE = (Int, Int)
  override type REWARD = Double

  private val maxStateValue = HopCountQRL.maxState
  private val bigInteger = HopCountQRL.inf
  /**
   * our standard gradient learning algorithm
   */
  class BaseHopCountAlgorithm() extends LearningBasedAlgorithm {

    override def reward(output: OUTPUT, state: STATE, oldState: STATE, action: ACTION): REWARD = {
      val realValue = node.get[java.lang.Double]("refHopCount").toInt
      val reward = Math.abs(realValue - output) match {
        case n if n >= 1 => -1
        case _ => 0
      }
      node.put("reward", reward * -1)
      reward
    }

    override def state(output: OUTPUT, oldState: STATE, action: ACTION): STATE = {
      val outputs = includingSelf.reifyField(nbr(output))
      val actions = includingSelf.reifyField(nbr(action))
      val minOutputEntity = outputs.minBy(_._2)
      val minOutput = minOutputEntity._2
      val minAction = actions(minOutputEntity._1)
      val recents = recentValues(3, (minOutput, minAction))
      val latests = recents.head
      val diff = latests._1 - minOutput
      val currentState = if(Math.abs(diff) > maxStateValue) {
        diff.signum * maxStateValue
      } else {
        diff
      }
      (currentState, latests._1)
    }

    override def run(input: (Boolean, () => ACTION), action: ACTION): OUTPUT = {
      val (source, metric) = input
      rep(bigInteger)(d =>
        mux(source) {
          0
        } {
          val neigh = excludingSelf.reifyField(nbr(d) + metric()).values ++ Set(bigInteger)
          neigh.min + action
        }
      )
    }
  }
}