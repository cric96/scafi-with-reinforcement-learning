package it.unibo.scafi.learning

trait DistributedRL[ID, S, A] {
  def all : Seq[(ID, QRLFacade[S, A])]
  def mine(id : ID) : QRLFacade[S, A]
}
