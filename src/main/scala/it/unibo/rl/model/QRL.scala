package it.unibo.rl.model

import it.unibo.rl.utils.Stochastics
import Stochastics._

trait QRL[S,A] {

  type R = Double // Reward
  type P = Double // Probability

  implicit val random = new scala.util.Random()

  /**
   * An Environment where one wants to learn
   */
  trait Environment extends ((S, A) => (R, S)) {
    def take(s: S, a: A): (R, S)

    override def apply(s: S, a: A) = take(s, a)
  }

  /**
   * An MDP is the idealised implementation of an Environment
   */
  trait MDP extends Environment {

    def transitions(s: S): Set[(A, P, R, S)]

    override def take(s: S, a: A): (R, S) =
      draw(cumulative(transitions(s).collect { case (`a`, p, r, s) => (p, (r, s)) }.toList))
  }

  /**
   * A strategy to act
   */
  type Policy = S=>A

  /**
   * A system configuration, where "runs" can occur
   */
  trait System {
    val environment: Environment
    val initial: S
    val terminal: S => Boolean

    def run(p: Policy): Stream[(A,S)]
  }

  /**
   * A state-value function
   */
  type VFunction = S=>R

  /**
   * Q is an updatable, table-oriented state-action value function, to optimise selection over certain actions
   */
  trait Q extends ((S,A)=>R) {
    def actions: Set[A]

    def update(s:S, a:A, v: R): Q

    def greedyPolicy: Policy = s => actions.maxBy{this(s,_)}

    def epsilonGreedyPolicy(epsilon: P): Policy = explorativePolicy(epsilon)

    def explorativePolicy(f: P): Policy = {
      case s if Stochastics.drawFiltered(_<f) => Stochastics.uniformDraw(actions)
      case s => greedyPolicy(s)
    }

    def optimalVFunction: VFunction = s => actions.map{ this(s,_) }.max
  }

  /**
   * The learning system, with parameters
   */
  trait LearningProcess {
    val system: System
    val gamma: Double // discount parameter
    val alpha: Double // step-size parameter
    val epsilon: Double
    val q0: Q

    def updateQ(s: S, qf: Q): (S, Q)
    def learn(episodes: Int, episodeLength: Int, qf: Q): Q
  }
}