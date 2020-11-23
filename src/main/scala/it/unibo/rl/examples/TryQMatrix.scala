package it.unibo.rl.examples

object TryQMatrix extends App {

  import QMatrix.Action._
  import QMatrix._

  val rl: QMatrix.Facade = Facade(
    width = 5,
    height = 5,
    initial = (0,0),
    terminal = {case _=>false},
    reward = { case ((1,0),_) => 10; case ((3,0),_) => 5; case _ => 0},
    jumps = { case ((1,0),_) => (1,4); case ((3,0),_) => (3,2) },
    gamma = 0.9,
    alpha = 0.5,
    epsilon = 0.3,
    v0 = 0
  )

  val q0 = rl.qFunction
  println(rl.show(q0.optimalVFunction,"%2.2f"))
  val q1 = rl.makeLearningInstance().learn(10000,100,q0)
  println(rl.show(q1.optimalVFunction,"%2.2f"))
  println(rl.show(s => actionToString(q1.greedyPolicy(s)),"%7s"))

}