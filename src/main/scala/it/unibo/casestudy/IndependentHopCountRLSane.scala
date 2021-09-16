package it.unibo.casestudy

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist.{ScafiAlchemistSupport, _}
import it.unibo.scafi.RLGradientLib

class IndependentHopCountRLSane extends AggregateProgram with StandardSensors with Gradients with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with RLGradientLib {
  lazy val LEFT_SRC: Int = intMoleculeOf("left_source")             // ID of the source at the left of the env (the stable one)
  lazy val RIGHT_SRC: Int = intMoleculeOf("right_source")           // ID of the source at the right of the env (the unstable one)
  lazy val RIGHT_SRC_STOP: Int = intMoleculeOf("stop_right_source") // time at which the source at the right of the env stops being a source
  private val hopCountMetric : Metric = () => 1.0

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
    val rlbasedHopCount : Int = 0
    // EXPORTS
    node.put("classicHopCount", classicHopCount)
    node.put("rlbasedHopCount", rlbasedHopCount)
    node.put(s"err_classicHopCount", Math.abs(refHopCount - classicHopCount))
    node.put(s"err_rlbasedHopCount", Math.abs(refHopCount - rlbasedHopCount))
    node.put(s"passed_time", passedTime)
    node.put("src", source)
  }

  private def intMoleculeOf(name : String) : Int = Integer.parseInt(node.get[Any](name).toString)
}
