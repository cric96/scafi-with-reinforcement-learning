package it.unibo.rl.model

import it.unibo.rl.utils.Stochastics
import Stochastics._

import scala.util.Random

trait QRL[S,A] {
  type R = Double // Reward
  type P = Double // Probability
  type Policy = S=>A
  type VFunction = S=>R

  implicit val random: Random
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