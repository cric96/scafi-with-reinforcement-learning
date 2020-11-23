package it.unibo.casestudy

import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.scafi.learning.QRLFacade

class GradientRLv2 extends AggregateProgram with StandardSensors with Gradients with ScafiAlchemistSupport
  with FieldCalculusSyntax with FieldUtils with CustomSpawn with RLGradientLib {
  //val LEFT_SRC = 5       // ID of the source at the left of the env (the stable one) in small grid
  //val RIGHT_SRC = 1      // ID of the source at the right of the env (the unstable one) in small grid
  val LEFT_SRC = 40       // ID of the source at the left of the env (the stable one)
  val RIGHT_SRC = 59      // ID of the source at the right of the env (the unstable one)
  lazy val RIGHT_SRC_STOP: Int = Integer.parseInt(node.get[Any]("stop_right_source").toString) // time at which the source at the right of the env stops being a source

  def range = node.get[Double]("range")

  type GInput = (Boolean, () => Double) // Gradient input
  type GOutput = Double

  override def main(): Any = {
    // SIMULATION DYNAMICS, BASELINE, and REFERENCE, IDEAL GRADIENT FOR COMPARISON AND ERROR CALCULATION
    val passedTime = timestamp()
    val source = if (mid == LEFT_SRC || (mid == RIGHT_SRC && passedTime < RIGHT_SRC_STOP)) true else false
    node.put("src", source)
    val classicG = classicGradient(source) // BASELINE
    val gWithoutRightSource = classicGradient(mid == LEFT_SRC) // optimal gradient when RIGHT_SRC stops beng a source
    val refGradient: Double = if(passedTime>=RIGHT_SRC_STOP) gWithoutRightSource else classicG

    // RL-BASED GRADIENT: THE OBJECT OF OUR EXPERIMENTS
    val rlbasedG = reinforcementLearning[GInput,GOutput,GAction,GState](new RLbasedGradient(), ConcentrateGQRL,
      Double.PositiveInfinity, NoIncrease, (source, () => nbrRange()), passedTime)

    // EXPORTS
    node.put("classicG", classicG)
    node.put("refG", refGradient)
    node.put("rlbasedG", rlbasedG)
    node.put(s"err_classicG", Math.abs(refGradient - classicG))
    node.put(s"err_rlbasedG", Math.abs(refGradient - rlbasedG))
    node.put(s"passed_time", passedTime)
  }

  trait RLBasedAlgorithm[I,O,A,S,R] {

    def run(input: I, action: A): O

    def state(o: O): S

    def reward(o: O, s: S, a: A): R
  }

  class RLbasedGradient extends RLBasedAlgorithm[(Boolean, () => Double), Double, GAction, GState, Double] {
    override def run(input: GInput, action: GAction): Double = {
      val (source, metric) = input
      rep(Double.PositiveInfinity)(d =>
        mux(source) {
          0.0
        } {
          minHoodPlus(nbr(d) + metric()) + action.increment
        })
    }

    override def state(o: Double): GState = {
      val myVariability = varianceFor(5, o)
      val variability = meanHood(nbr(myVariability))
      node.put("myvar", myVariability)
      node.put("nbrvar", variability)

      val rvs = recentValues(k=3, o)
      node.put("debug_recentVals", rvs)
      node.put("debug_isIncreasing", isIncreasing(o, k=3))
      node.put("debug_delta", delta(o, k=3))
      node.put("debug_isStable", isStable(o, k=3))
      node.put("debug_myvariance", myVariability)

      //& delta(o, k=3)<range/10
      val state = if(isIncreasing(o, k=3) & delta(o, k=3)<range/10) RisingSlowlyState else NormalState
      node.put("debug_state", recentValues(k=10, state))
      state
    }

    override def reward(o: Double, s: GState, a: GAction): Double = {
      var reward = 0
      if(isStable(o, k=3)) reward = 1
      else if(s.equals(RisingSlowlyState) && a.equals(IncreaseMuch)) reward = 50
      else if(s.equals(RisingSlowlyState) && a.equals(NoIncrease)) reward = -5
      else if(s.equals(NormalState) && a.equals(NoIncrease)) reward = 5
      else if(s.equals(NormalState) && a.equals(IncreaseMuch)) reward = -2
      else reward = -1
      node.put("debug_reward", recentValues(k=10, reward))
      reward
    }
  }

  def reinforcementLearning[I,O,A,S](algorithm: RLBasedAlgorithm[I,O,A,S,Double],
                                     q: QRLFacade[S,A], o0: O, a0: A, input: I, t: Double, learn: Boolean = true): O = {
    rep((o0, a0)) { case (o, a) => {
      val action = branch(!learn) {
        q.qlearning.setState(algorithm.state(o))
        q.qlearning.takeGreedyAction(q.qTable)
      } {
        //println(s"o: $o, a: $a")
        val state = algorithm.state(o)
        //println(s"qtable first: ${q.qTable}")
        val reward = algorithm.reward(o, state, a)
        q.qTable = q.qlearning.observeEnvAndUpdateQ(q.qTable, state, reward, t)
        val action = q.qlearning.takeEpsGreedyAction(q.qTable, t)
        node.put("state", state)
        node.put("Qtable", ConcentrateGQRL.qTable)
        node.put("action", action)

        //println(s"State: $state, Action: $a, reward: $reward, GreedyAction: $action")
        //println(s"qtable: ${q.qTable}")

        action
      }

      val output = algorithm.run(input, action)

      (output, action)
    }
    }._1
  }

}