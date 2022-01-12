package it.unibo.rl.model

import scala.util.Random

trait QRLImpl[S,A] extends QRL[S,A] with Serializable {

  /**
   * A Map-based implementation, with defaults for terminal and unexplored states
   * @param actions
   * @param v0
   * @param terminal
   * @param terminalValue
   */
  case class QFunction(
    override val actions: Set[A],
    v0: R = 0.0,
    @transient terminal: S=>Boolean = (s:S)=>false,
    terminalValue: Double = 0.0) extends Q with Serializable {
    val map: collection.mutable.Map[(S,A),R] = collection.mutable.Map()
    override def apply(s: S, a: A) = if (terminal(s)) terminalValue else map.getOrElse(s->a,v0)
    override def update(s: S, a: A, v: Double): Q = { map += ((s->a)->v); this }
    override def toString = map.toString
  }

  case class RealtimeQLearning(
                        gamma: Double,
                        q0: Q){
    private var state: Option[S] = None
    private var action: Option[A] = None

    def setState(s: S): Unit = { state = Some(s) }
    def takeAction(a: A): Unit = { action = Some(a) }
    def takeEpsGreedyAction(qf: Q, time: Double)(implicit rand: Random): A = {
      val epsilon = QLParameter(0,time).epsilon
      //println(s"epsilon value: $epsilon at time $time, st: ${state.get}")
      val a = qf.explorationPolicy(epsilon)(rand)(state.get)
      takeAction(a)
      a
    }
    def takeGreedyAction(qf: Q): A = qf.greedyPolicy(state.get)
    def observeEnvAndUpdateQ(qf: Q, newState: S, reward: R, time: Double): Q = try {
      val alpha = QLParameter(0, time).alpha
      //println(s"alpha value $alpha at time $time")
      (for(
        s <- state;
        a <- action
      ) yield {
        // By wikipedia (weighted average)
        //val vr = (1 - alpha) * qf(s, a) + alpha * (reward + gamma * qf.optimalVFunction(newState))
        // By book
        val update = (reward + gamma * qf.optimalVFunction(newState) - qf(s,a))

        val vr = qf(s, a) + (if(update >= 0) {
          update * alpha
        } else {
          update * (alpha)
        }
        )
        //println(s"State: $s, Action: $a, Value: $vr, Alpha: $alpha, Pair: ${qf(s,a)}, optimal: ${qf.optimalVFunction(newState)} ")
        //println(s"Value: $vr, optimal: ${qf.optimalVFunction(newState)} ")
        qf.update(s, a, vr)
      }).getOrElse(q0)
    } finally {
      setState(newState)
    }
  }
}

case class QLParameter(value: Double, t: Double) {
  def epsilon = 0.01 // Math.min(0.9, 0.00 + (1- 0.00) * math.exp(-0.4 * (t/ 20.0)))
  def alpha = 0.9 // Math.max(0.05, Math.min(0.5, 0.1 - Math.log10((t+1)/100.0))) //
}