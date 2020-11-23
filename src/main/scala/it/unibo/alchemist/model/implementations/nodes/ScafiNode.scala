package it.unibo.alchemist.model.implementations.nodes

import it.unibo.alchemist.model.interfaces.{Environment, Molecule, Position}

class ScafiNode[T, P<:Position[P]](env: Environment[T, P]) extends AbstractNode[T](env) {

  override def getConcentration(mol: Molecule): T = try {
    super.getConcentration(mol)
  } catch {
    case exc:Exception => throw new Exception(s"Cannot get concentration of molecule $mol in node ${this.getId}", exc)
  }

  override def createT = throw new IllegalStateException("The molecule does not exist and cannot create empty concentration")

}
