package it.unibo.rl.examples

import it.unibo.rl.model.QRLImpl

object QMatrix {

  type Node = (Int,Int)
  object Action extends Enumeration {
    val LEFT,RIGHT,UP,DOWN = Value
    val actionToString = Map(LEFT->"<", RIGHT->">", UP->"^", DOWN->"v")
  }
  import Action._
  type Action = Action.Value

  case class Facade(
     width: Int,
     height: Int,
     initial: Node,
     terminal: PartialFunction[Node,Boolean],
     reward:  PartialFunction[(Node,Action),Double],
     jumps: PartialFunction[(Node,Action),Node],
     gamma: Double,
     alpha: Double,
     epsilon: Double = 0.0,
     v0: Double) {
    val qrl = new QRLImpl[Node,Action]{ }
    import qrl._

    def qEnvironment(): Environment = new Environment {
      override def take(s: Node, a: Action): (R, Node) = {
        // applies direction, without escaping borders
        val n2: Node = (s,a) match {
          case ((n1,n2),UP) => (n1, (n2-1) max 0)
          case ((n1,n2),DOWN) => (n1, (n2+1) min (height-1))
          case ((n1,n2),LEFT) => ((n1-1) max 0, n2)
          case ((n1,n2),RIGHT) => ((n1+1) min (width-1), n2)
          case _ => ???
        }
        // computes rewards, and possibly a jump
        (reward.apply((s,a)),
          jumps.orElse[(Node,Action),Node]{case _=>n2}.apply((s,a)))
      }
    }

    def qFunction = QFunction(Action.values,v0,terminal)

    def qSystem = QSystem(
      environment = qEnvironment(),
      initial,
      terminal
    )

    def makeLearningInstance() = QLearning(
      qSystem, gamma, alpha, epsilon, qFunction
    )

    def show[E](v: Node=>E, formatString:String): String = {
      (for (row <- 0 until height;
            col <- 0 until width)
        yield (formatString.format(v((col,row))) + (if (col == width-1) "\n" else "\t"))).mkString("")
    }
  }

}
