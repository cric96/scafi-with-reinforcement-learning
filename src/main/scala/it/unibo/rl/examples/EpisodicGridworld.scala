package it.unibo.rl.examples

object EpisodicGridworld extends App {

  import QMatrix.Action._
  import QMatrix._

  val rl: QMatrix.Facade = Facade(
    width = 4,
    height = 4,
    initial = (0,3),
    terminal = {case (0,0) | (3,3) => true; case _ => false }, // episodic task
    reward = { case _ => -1 },
    jumps = PartialFunction.empty,
    gamma = 1.0, // undiscounted
    alpha = 0.9,
    epsilon = 1, // equiprobable random policy
    v0 = 0
  )

  val q0 = rl.qFunction
  println(rl.show(q0.optimalVFunction,"%2.2f"))
  val q1 = rl.makeLearningInstance().learn(10000,1000, q0)
  println(rl.show(q1.optimalVFunction,"%2.2f"))
  println(rl.show(s => actionToString(q1.greedyPolicy(s)),"%7s"))

}