package it.unibo.casestudy
/**
 *  distributed reinforcement learning version for the gradient problem. Each node in the aggregate environment has
 *  its GQRL instance using to perform local learning (that could be influenced by its neighbours).
 *  Essentially, it is a map that contains for each ID the GQRL instance.
* */
object DistributedGQRL {
  private var qLearningMap : Map[Any, GQRL] = Map.empty
  /**
   * return the GQRL instance for a specified id
   */
  def mine(id : Any) : GQRL = {
    qLearningMap = qLearningMap.get(id) match {
      case None => qLearningMap + (id -> new GQRL)
      case _ => qLearningMap
    }
    qLearningMap(id)
  }
}
