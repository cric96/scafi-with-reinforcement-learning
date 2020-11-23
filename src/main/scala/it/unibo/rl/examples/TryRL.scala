package it.unibo.rl.examples

import it.unibo.rl.examples.TwoWaysMDP.State

object TryRL extends App {
  val rl = TwoWaysMDP.rl.rlTW()
  var qf = TwoWaysMDP.rl.qfTW()
  qf = rl.learn(20,10,qf)
  (-5 to 10).map(i => (i,
    qf.actions.maxBy{qf(State(i),_)},
    qf.actions.map{qf(State(i),_)}.max))
    .foreach(println(_))
}
