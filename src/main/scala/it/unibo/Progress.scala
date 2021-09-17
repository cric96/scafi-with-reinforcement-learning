package it.unibo

import it.unibo.alchemist.model.implementations.actions.AbstractLocalAction
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces._
import it.unibo.alchemist.utils.LearningInstances
import org.apache.commons.math3.random.RandomGenerator

sealed class Progress[T, P <: Position[P]](
                                          environment: Environment[T, P],
                                          node: Node[T],
                                          reaction: Reaction[T],
                                          rng: RandomGenerator
                                        ) extends AbstractLocalAction[T](node) {
  override def cloneAction(n: Node[T], r: Reaction[T]): Action[T] = new Progress(environment,node,reaction,rng)
  var run = false // NOTE: this workaround shouldn't be necessary

  def episode: Int = node.getConcentration(new SimpleMolecule("episode")).asInstanceOf[Double].toInt

  def printEvery: Int = node.getConcentration(new SimpleMolecule("print_every")).asInstanceOf[Int]

  override def execute(): Unit = {
    if(run) {
      afterFirstFire()
    }
    run = true
  }

  def afterFirstFire(): Unit = if(episode % printEvery == 0){
    println(episode)
  } else {
    print("-")
  }
}