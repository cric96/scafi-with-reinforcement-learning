package it.unibo.casestudy

import it.unibo.alchemist.model.interfaces.{Position, Environment => AlchemistEnv}
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.rl.examples.QMatrix.Action.Value

import scala.collection.immutable.Queue

class GradientRL extends AggregateProgram with StandardSensors with Gradients with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with RLGradientLib {
  var nEpisodes = 0

  val LEFT_SRC = 40
  val RIGHT_SRC = 59

  val RIGHT_SRC_STOP = 30
  val REFRESH_DELAY = 5
  val EPISODE_LEN = 120

  val STOP_LEARNING_T = 50000

  def range = node.get[Double]("range")

  override def main(): Any = {
    val k = rep(0)(_+1)
    val startTime = rep(timestamp())(x => x)
    val passedTime = timestamp()-startTime
    branch(passedTime%EPISODE_LEN<=REFRESH_DELAY && passedTime<STOP_LEARNING_T){
      // Time for all to "refresh" (i.e., start a new episode by re-entering the else branch)
      node.put("classicG", 0)
      node.put("refG", 0)
      node.put("rlbasedG", 0)
    } {
      val epStartTime = rep(timestamp())(x => x)
      val epPassedTime = timestamp()-epStartTime
      val n = rep(nEpisodes+1)(x => x); nEpisodes = n
      val source = if (mid == LEFT_SRC || (mid == RIGHT_SRC && epPassedTime < RIGHT_SRC_STOP)) true else false
      val classicG = classicGradient(source)

      val gWithoutRightSource = classicGradient(mid == LEFT_SRC) // optimal gradient when RIGHT_SRC stops beng a source
      val refGradient: Double = if(epPassedTime>=RIGHT_SRC_STOP) gWithoutRightSource else classicG
      node.put("src", source)

      val rlbasedG = rlGradient(source, Some(refGradient), passedTime>=STOP_LEARNING_T)
      node.put("classicG", classicG)
      node.put("refG", refGradient)
      node.put("rlbasedG", rlbasedG)


      node.put(s"c_${n}_classicG", classicG)
      node.put(s"c_${n}_rlbasedG", rlbasedG)
      node.put(s"c_${n}_refG", gWithoutRightSource)

      node.put(s"e_${n}_classicG_err", gWithoutRightSource-classicG)
      node.put(s"e_${n}_rlbasedG_err", gWithoutRightSource-rlbasedG)
    }
  }

  def rlGradient(source: Boolean, refg: Option[Double], go: Boolean, metric: () => Double = nbrRange _) = {
    val k = 1
    rep(Double.PositiveInfinity){ case d =>
      mux(source){ 0.0 }{
        val stable = varianceFor(2, d)==0
        val myVariability = varianceFor(5, d)
        val variability = meanHood(nbr(myVariability))
        node.put("myvar", myVariability)
        node.put("nbrvar", variability)
        val g = minHoodPlus(nbr(d)+metric())

        minHoodPlus(nbr(d)+metric()+rl(
          state = if(isIncreasing(g) & delta(g)<range/2) RisingSlowlyState else NormalState,
          if(!refg.isEmpty){
            val optg = refg.get
            //if(d > optg+range/2) -15 else if(Math.abs(d-optg) < range/10) +10 else -1
            if(stable) 20 else -1
          } else -1,
          go
        ).increment)
      }
    }
  }

  def rl(state: GState, reward: Double, go: Boolean): GAction = {
    if(go){
      ConcentrateGQRL.qlearning.setState(state)
      ConcentrateGQRL.qlearning.takeGreedyAction(ConcentrateGQRL.qTable)
    } else {
      // TODO: add 0.0 equals time. New configuration present in GradientRLv2 an ConcentrateGQRL
      ConcentrateGQRL.qTable = ConcentrateGQRL.qlearning.observeEnvAndUpdateQ(ConcentrateGQRL.qTable, state, reward, 0.0)
      val action = ConcentrateGQRL.qlearning.takeEpsGreedyAction(ConcentrateGQRL.qTable, 0.0)
      node.put("state", state)
      node.put("Qtable", ConcentrateGQRL.qTable)
      node.put("action", action)
      action
    }
  }
}

trait RLGradientLib { self: AggregateProgram with FieldUtils with ScafiAlchemistSupport =>

  // TODO: add in ScaFi stdlib
  def delta(value: Double, k: Int = 2, default: Double = Double.PositiveInfinity): Double = {
    val vs = recentValues(k, value)
    if(vs.drop(k-1).isEmpty) default else vs.last-vs.head
  }

  // TODO: add in ScaFi stdlib
  def deltas[T:Numeric](value: T, k: Int = 2, default: Boolean = false): Iterable[T] = {
    val vs = recentValues(k, value)
    if(vs.drop(1).isEmpty)
      List()
    else
      vs.sliding(size=2, step=1)
        .map(q => implicitly[Numeric[T]].minus(q.head, q.drop(1).head)).toList
  }

  // TODO: add in ScaFi stdlib
  def isIncreasing[T : Ordering](value: T, k: Int = 2, default: Boolean = false): Boolean = {
    val vs = recentValues(k, value)
    if(vs.drop(1).isEmpty)
      default
    else {
      vs.sliding(size = 2, step = 1)
        .map(q => implicitly[Ordering[T]].compare(q.head, q.drop(1).head))
        .sum < 0
    }

  }

  // TODO: add in ScaFi stdlib
  def isStable[T](value: T, k: Int = 2): Boolean = {
    val r = recentValues(k, value).forall(_ == value)
    //println(s"value ${r}")
    r
  }
  // TODO: add in ScaFi stdlib
  def previous[T](value : T): Option[T] =
    recentValues(2, value).dropRight(1).headOption

  // TODO: add in ScaFi stdlib
  def meanHood(v: Double): Double =
    excludingSelf.sumHood(nbr(v)) / excludingSelf.sumHood(1)

  // TODO: add in ScaFi stdlib
  def varianceFor(k: Int, value: Double): Double = {
    val vs = recentValues(k, value)
    val len = vs.length
    val mean = vs.sum / len
    vs.foldLeft(0.0)((acc,v) => acc+Math.pow(v-mean, 2)) / len
  }

  // TODO: add in ScaFi stdlib
  def recentValues[T](k: Int, value: T): Queue[T] =
    rep((Queue[T]())){ case (vls) => {
      (if(vls.size==k) vls.tail else vls) :+ value
    }}
}