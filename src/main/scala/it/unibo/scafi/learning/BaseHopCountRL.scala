package it.unibo.scafi.learning

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{ScafiAlchemistSupport, _}
import it.unibo.casestudy.HopCountQRL
import it.unibo.scafi.RLGradientLib

abstract class BaseHopCountRL extends AggregateProgram with StandardSensors with Gradients with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with RLGradientLib with AggregateProgramWithLearning{

  override type OUTPUT = Int
  override type INPUT = (Boolean, () => Int)

  lazy val episode: Double = node.get[java.lang.Double]("episode")
  lazy val episodeLength: Int = node.get[java.lang.Integer]("episode_length")
  lazy val haveToLearn: Boolean = node.get[java.lang.Boolean]("learn")
  lazy val clockFactor: Int = 100
  //Variable loaded by alchemist configuration.
  lazy val LEFT_SRC: Int = intMoleculeOf("left_source")       // ID of the source at the left of the env (the stable one)
  lazy val RIGHT_SRC: Int = intMoleculeOf("right_source")      // ID of the source at the right of the env (the unstable one)
  lazy val RIGHT_SRC_STOP: Int = intMoleculeOf("stop_right_source") // time at which the source at the right of the env stops being a source
  lazy val WINDOW: Int = 3
  lazy val TRAJECTORY: Int = 7
  private val hopCountMetric : Metric = () => 1.0

  def algorithm : LearningBasedAlgorithm
  def initialSetup : (OUTPUT, ACTION, STATE)
  def learningInstance : QRLFacade[STATE, ACTION]
  def intMoleculeOf(name : String) : Int = (node.get[Integer](name))

  override def main(): Any = {
    // SIMULATION DYNAMICS, BASELINE, and REFERENCE, IDEAL GRADIENT FOR COMPARISON AND ERROR CALCULATION
    val passedTime = alchemistTimestamp.toDouble
    val source = if (mid == LEFT_SRC || (mid == RIGHT_SRC && passedTime < RIGHT_SRC_STOP)) true else false
    val classicHopCount = classicGradient(source, hopCountMetric) // BASELINE
    val hopCountWithoutRightSource = classicGradient(mid == LEFT_SRC, hopCountMetric) // optimal gradient when RIGHT_SRC stops being a source
    val refHopCount = if(passedTime >= RIGHT_SRC_STOP) hopCountWithoutRightSource else classicHopCount
    // Can be used in the learning session
    node.put("refHopCount", refHopCount)
    // RL-BASED GRADIENT: THE OBJECT OF OUR EXPERIMENTS
    val clock = (passedTime + episode * episodeLength) / clockFactor

    val rlbasedHopCount =
      if(haveToLearn){
        learn(algorithm)((source, () => hopCountMetric().toInt))(initialSetup)(clock)
      } else {
        act(algorithm)((source, () => hopCountMetric().toInt))(initialSetup)(clock)
      }
    // EXPORTS
    node.put("classicHopCount", classicHopCount)
    node.put("rlbasedHopCount", rlbasedHopCount)
    node.put(s"err_classicHopCount", Math.abs(refHopCount - classicHopCount))
    node.put(s"err_rlbasedHopCount", Math.abs(refHopCount - rlbasedHopCount))
    node.put(s"passed_time", passedTime)
    node.put("src", source)
  }
}

/**
 * a standard target version of our learning gradient algorithm. It increase the output gradient by the action increment
 * specified.
 */
trait BaseHopCountAlgorithm {
  self : BaseHopCountRL =>

  override type ACTION = Int
  override type STATE = List[Int]
  override type REWARD = Double

  private val bigInteger = HopCountQRL.inf
  /**
   * our standard gradient learning algorithm
   */
  class BaseHopCountAlgorithm() extends LearningBasedAlgorithm {

    override def reward(output: OUTPUT, state: STATE, oldState: STATE, action: ACTION): REWARD = {
      val realValue = node.get[java.lang.Double]("refHopCount").toInt
      val reward = Math.abs(realValue - output) match {
        case n if n == 0 => -1
        case _ => 0
      }
      node.put("reward", reward * -1)
      reward
    }

    override def state(output: OUTPUT, oldState: STATE, action: ACTION): STATE = {
      val outputs = includingSelf.reifyField(nbr(output))
      val minOutputEntity = outputs.minBy(_._2)
      val minOutput = minOutputEntity._2
      val recent = recentValues(WINDOW, minOutput)
      val oldState = recent.head
      val diff = minOutput - oldState
      val recentsDiffs = recentValues(TRAJECTORY, diff).toList
      //List(minOutput)
      recentsDiffs
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