package it.unibo

import java.io.{BufferedOutputStream, BufferedWriter, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream, OutputStreamWriter}
import java.nio.file.{Files, Path}

import it.unibo.alchemist.model.implementations.actions.AbstractLocalAction
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces._
import it.unibo.casestudy.ConcentrateGQRL
import org.apache.commons.math3.random.RandomGenerator

sealed class LoadQF[T, P <: Position[P]](
                                          environment: Environment[T, P],
                                          node: Node[T],
                                          reaction: Reaction[T],
                                          rng: RandomGenerator,
                                          fileName: String,
                                        ) extends AbstractLocalAction[T](node) {
  override def cloneAction(n: Node[T], r: Reaction[T]): Action[T] = new LoadQF(environment,node,reaction,rng,fileName)
  var run = false // NOTE: this workaround shouldn't be necessary

  def episode = node.getConcentration(new SimpleMolecule("episode")).asInstanceOf[Double].toInt

  override def execute(): Unit = if(!run && episode==0){
    /*println(s"Time ${environment.getSimulation.getTime} Node ${node.getId} episode ${node.getConcentration(new SimpleMolecule("episode"))} - learn? " +
      s"${node.getConcentration(new SimpleMolecule("learn"))} => Loading from ${fileName}")
     */
    if(Files.exists(Path.of(s"bootstrap"))) {
      val oos = new ObjectInputStream(new FileInputStream(s"bootstrap"))
      for (s <- ConcentrateGQRL.states;
           a <- ConcentrateGQRL.actions) {
        ConcentrateGQRL.qTable.update(s, a, oos.readDouble())
      }
      println(s"Episode $episode - Read QTable:\n${ConcentrateGQRL.qTable}\n")
      oos.close()
    }

    run = true
  }
}

sealed class SaveQF[T, P <: Position[P]](
                                          environment: Environment[T, P],
                                          node: Node[T],
                                          reaction: Reaction[T],
                                          rng: RandomGenerator,
                                          fileName: String,
                                        ) extends AbstractLocalAction[T](node) {
  override def cloneAction(n: Node[T], r: Reaction[T]): Action[T] = new LoadQF(environment,node,reaction,rng,fileName)
  var run = false // NOTE: this workaround shouldn't be necessary

  def episode = node.getConcentration(new SimpleMolecule("episode")).asInstanceOf[Double].toInt

  def saveEvery = node.getConcentration(new SimpleMolecule("save_every")).asInstanceOf[Int]

  override def execute(): Unit = if(!run && episode>0 && episode%saveEvery==0){
    /*println(s"Time ${environment.getSimulation.getTime} Node ${node.getId} episode ${} - learn? " +
      s"${node.getConcentration(new SimpleMolecule("learn"))} => Saving to ${fileName}")*/

    val oos = new ObjectOutputStream(new FileOutputStream(s"qtable-${episode}"))
    val bos = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(s"qtable-${episode}.txt")))
    for(s <- ConcentrateGQRL.states;
        a <- ConcentrateGQRL.actions){
      oos.writeDouble(ConcentrateGQRL.qTable(s,a))
      bos.write(s"($s,$a) = ${ConcentrateGQRL.qTable(s,a)}\n")
    }
    println(s"Episode $episode - Saved QTable:\n${ConcentrateGQRL.qTable}\n")
    oos.close()
    bos.close()

    /*
    import play.api.libs.json._

    //implicit val qtableWrites: Writes[ConcentrateGQRL.system.qrl.QFunction] = (
    //  (JsPath \ "actions").write[Set[ConcentrateGQRL.Action]]
    //)(unlift(ConcentrateGQRL.system.qrl.QFunction.unapply))

    implicit object ActionFormat extends Format[ConcentrateGQRL.Action] {
      override def reads(json: JsValue): JsResult[ConcentrateGQRL.Action] = JsSuccess((json \ "actionName") match {
        case JsDefined(JsString("noIncrease")) => NoIncrease((json \ "step").get.as[Int])
        case JsDefined(JsString("increase")) => Increase((json \ "step").get.as[Int])
        case _: JsUndefined => throw new Exception("Undefined action name")
      })

      override def writes(o: ConcentrateGQRL.Action): JsValue = o match {
        case ConcentrateGQRL.NoIncrease(k) => Json.obj("actionName" -> JsString("noIncrease"), "step" -> JsNumber(k))
        case ConcentrateGQRL.NoIncrease(k) => Json.obj("actionName" -> JsString("increase"), "step" -> JsNumber(k))
      }
    }

    implicit object QtableFormat extends Format[ConcentrateGQRL.system.qrl.QFunction] {
      override def reads(json: JsValue): JsResult[system.qrl.QFunction] = JsSuccess(system.qrl.QFunction(
        (json \ "actions").as[Set[ConcentrateGQRL.Action]]
      ))

      override def writes(o: system.qrl.QFunction): JsValue = Json.obj(
        "actions" -> o.actions
      )
    }

    val json = Json.toJson(ConcentrateGQRL.qTable.asInstanceOf[ConcentrateGQRL.system.qrl.QFunction])
    println(json)
    */

    run = true
  }
}