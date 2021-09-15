package it.unibo.rl.model

import scala.annotation.tailrec

trait QRLImpl[S,A] extends QRL[S,A] {

  /**
   * MDP factories
   */
  object MDP {

    def ofFunction(f: PartialFunction[S, Set[(A, P, R, S)]]): MDP =
      new MDP {
        override def transitions(s: S) = f.applyOrElse(s, (x: S) => Set())
      }

    def ofRelation(rel: Set[(S, A, P, R, S)]): Environment = ofFunction {
      case s => rel filter {
        _._1 == s
      } map { t => (t._2, t._3, t._4, t._5) }
    }

    def ofTransitions(rel: (S, A, P, R, S)*): Environment = ofRelation(rel.toSet)

    def ofOracle(oracle: (S, A) => (R, S)): Environment = new Environment {
      override def take(s: S, a: A): (R, S) = oracle(s, a)
    }
  }

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

  case class QSystem(
    override val environment: Environment,
    override val initial: S,
    override val terminal: S => Boolean) extends System{

    final override def run(p: Policy): Stream[(A,S)] = {
      val a0 = p(initial)
      Stream.iterate( (initial,a0,initial)){ case (_,a,s2) => val a2 = p(s2); (s2,a2,environment.take(s2,a2)._2) }
        .tail
        .takeWhile {case (s1,_,_) => !terminal(s1)}
        .map {case (_,a,s2) => (a,s2)}
    }
  }

  case class RealtimeQLearning(
                        val gamma: Double,
                        //val alpha: Double,
                        //val epsilon: Double,
                        val q0: Q){
    private var state: Option[S] = None
    private var action: Option[A] = None

    def setState(s: S) { state = Some(s) }
    def takeAction(a: A) { action = Some(a) }
    def takeEpsGreedyAction(qf: Q, time: Double): A = {
      val epsilon = QLParameter(0,time).epsilon
      //println(s"epsilon value: $epsilon at time $time, st: ${state.get}")
      val a = qf.explorativePolicy(epsilon)(state.get)
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
        val vr = qf(s, a) + alpha *(reward + gamma * qf.optimalVFunction(newState) - qf(s,a))
        //println(s"State: $s, Action: $a, Value: $vr, Alpha: $alpha, Pair: ${qf(s,a)}, optimal: ${qf.optimalVFunction(newState)} ")
        //println(s"Value: $vr, optimal: ${qf.optimalVFunction(newState)} ")
        qf.update(s, a, vr)
      }).getOrElse(q0)
    } finally {
      setState(newState)
    }
  }

  case class QLearning(
    override val system: QSystem,
    override val gamma: Double,
    override val alpha: Double,
    override val epsilon: Double,
    override val q0: Q) extends LearningProcess {

      override def updateQ(s: S, qf: Q): (S,Q) = {
        val a = qf.explorativePolicy(epsilon)(s)
        val (r,s2) = system.environment.take(s,a)
        val vr = (1-alpha)*qf(s,a) + alpha*(r + gamma * qf.optimalVFunction(s2))
        val qf2 = qf.update(s,a,vr)
        (s2,qf2)
      }

      @tailrec
      final override def learn(episodes: Int, episodeLength: Int, qf: Q): Q = {
        @tailrec
        def runSingleEpisode(in: (S,Q), episodeLength: Int): (S,Q) =
          if (episodeLength==0 || system.terminal(in._1)) in else runSingleEpisode(updateQ(in._1,in._2),episodeLength-1)
        episodes match {
          case 0 => qf
          case _ => learn( episodes-1, episodeLength, runSingleEpisode( (system.initial,qf), episodeLength)._2)
        }
      }
  }
}

case class QLParameter(value: Double, t: Double) {
  def epsilon = Math.min(0.9, 0.1 + (1- 0.00) * math.exp(-0.05 * t))
  def alpha = Math.max(0.001, Math.min(0.5, 0.1 - Math.log10((t+1)/100.0))) //

}