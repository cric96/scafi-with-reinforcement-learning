package it.unibo.casestudy

trait Initialization[S, A] {
  def getState: Set[S]
  def getAction: Set[A]
}

class InitializationImpl extends Initialization[GState, GAction]{
  override def getState: Set[GState] = {
    val states = Set[GState](NormalState, RisingSlowlyState)
    for(s <- states) yield s
  }
  override def getAction: Set[GAction] = {
    val actions = Set[GAction](NoIncrease, IncreaseLittle, IncreaseMuch)
    for (a <- actions) yield a
  }
}
