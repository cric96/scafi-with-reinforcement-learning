package it.unibo.scafi.learning

import it.unibo.rl.model.QRLImpl
//TODO fix this concept, a remnant of the past. Try to clarify the concept.
class QRLFacade[S,A](val qrl: QRLImpl[S,A], val actions: Set[A]) {
  case class Facade[T,P](gamma: Double,
                         v0: Double){
    import qrl._
    def qFunction: QFunction = QFunction(actions, v0)
    def makeLearningInstance(): RealtimeQLearning = RealtimeQLearning(gamma, qFunction)
  }

  val system = Facade(gamma = 0.90, v0 = 0)
  val qlearning = system.makeLearningInstance()
  var qTable = qlearning.q0
}