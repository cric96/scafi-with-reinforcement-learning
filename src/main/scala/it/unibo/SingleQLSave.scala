package it.unibo

import java.io._

import it.unibo.alchemist.model.implementations.actions.AbstractLocalAction
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces._
import it.unibo.scafi.learning.QRLFacade

sealed class SingleQLSave[T, P <: Position[P], S, A](
                                          environment: Environment[T, P],
                                          node: Node[T],
                                          reaction: Reaction[T],
                                          facade : QRLFacade[S, A],
                                          states : Iterable[S]
                                        ) extends AbstractLocalAction[T](node) {
  override def cloneAction(n: Node[T], r: Reaction[T]): Action[T] = new SingleQLSave(environment,node,reaction, facade, states)

  def episode = node.getConcentration(new SimpleMolecule("episode")).asInstanceOf[Double].toInt

  def saveEvery : Int = node.getConcentration(new SimpleMolecule("save_every")).asInstanceOf[Int]

  override def execute(): Unit = if(episode != 0 && episode % saveEvery == 0){

    val oos = new ObjectOutputStream(new FileOutputStream(s"qtable-${episode}"))
    val bos = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(s"qtable-${episode}.txt")))
    for(state <- states;
        action <- facade.qTable.actions) {
      bos.write(s"($state,$action) = ${facade.qTable(state, action)}\n")
    }
    println(s"Episode $episode - Saved QTable:\n${facade.qTable}\n")
    oos.close()
    bos.close()
  }
}