package it.unibo.casestudy

import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.scafi.ScafiIncarnationForAlchemist._
import it.unibo.rl.model.QRLImpl
import it.unibo.rl.utils.Clock
import it.unibo.scafi.learning.{AggregateProgramWithLearning, QLearningCentralizedProgram, QRLFacade}

case object NoState
sealed trait Action
case object TConstruct extends Action
case object GConstruct extends Action
case object CConstruct extends Action

object TGCRL extends QRLFacade[NoState.type, Action](
  new QRLImpl[NoState.type , Action] {},
  actions = Set(TConstruct, GConstruct, CConstruct)
)

trait TGCAlgorithm {
  self : AggregateProgram with AggregateProgramWithLearning with BlockT with BlockG with BlockC with StandardSensors with ScafiAlchemistSupport =>
  override type OUTPUT = Double
  override type ACTION = Action
  override type STATE = NoState.type
  override type REWARD = Double
  def source : Boolean = if(mid() == sourceId) true else false
  def sourceId : ID
  val algorithm = new LearningBasedAlgorithm {
    override def run(input: INPUT, action: ACTION): OUTPUT = {
      val t = T(10)
      val g = distanceTo(source)
      val c = C[Double, Int](g, _ + _, 1, 0)
      action match {
        case TConstruct => t
        case GConstruct => g
        case CConstruct => c
      }
    }

    override def state(output: OUTPUT, oldState: STATE, action: ACTION): STATE = NoState

    override def reward(output: OUTPUT, state: STATE, oldState: STATE, action: ACTION): REWARD = {
      def node(id : ID) : Node[Any] = alchemistEnvironment.getNodeByID(id)

      val value = output - alchemistEnvironment.getDistanceBetweenNodes(node(mid()), node(sourceId))
      if(value.isInfinity) {
        0.0
      } else {
        - Math.abs(value)
      }
    }
  }
}

class AggregateProgramTGC extends AggregateProgram with QLearningCentralizedProgram
  with BlockT with BlockC with BlockG with StandardSensors with ScafiAlchemistSupport with TGCAlgorithm{
  override type FACADE = QRLFacade[NoState.type , Action]
  override val learningContext: FACADE = TGCRL
  override type ACTION = Action
  override type STATE = NoState.type
  override type INPUT = Unit
  override type OUTPUT = Double
  val initialAction = learningContext.qlearning.q0.greedyPolicy(NoState)

  def randomOne = List(TConstruct, GConstruct, CConstruct)(randomGen.nextInt(3))
  override def main(): Any = {
    val result = learn(algorithm)({})((Double.PositiveInfinity, initialAction, NoState))(Clock.timestampSec())
    node.put("error", Math.abs(algorithm.reward(result, NoState, NoState, TConstruct)))
  }

  override def sourceId: Int = node.get[Integer]("source_id")
}
