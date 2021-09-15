package it.unibo

import java.io.{BufferedOutputStream, BufferedWriter, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream, OutputStreamWriter}
import java.nio.file.{Files, Path}
import it.unibo.alchemist.model.implementations.actions.AbstractLocalAction
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces._
import it.unibo.casestudy.{ConcentrateGQRL, IndependentHopCountRL}
import org.apache.commons.math3.random.RandomGenerator

import scala.collection.JavaConverters.asScalaIteratorConverter

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
    val tables = this.environment.getNodes.iterator().asScala.toList.map(node => IndependentHopCountRL.mine(node.getId))
    val states = tables.head.states
    val actions = tables.head.actions
    val globalTable = states.toList.map(state => (state, actions.toList.map(action => tables.map(_.qTable(state, action)).sum / tables.size)))
    val head = List("/") ++ actions.map(_.toString)
    val rows = globalTable.map { case (state, values) => state.toString() :: values.map(_.toString) }
    println(Tabulator.format(head :: rows))
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

object Tabulator {
  def format(table: Seq[Seq[Any]]) = table match {
    case Seq() => ""
    case _ =>
      val sizes = for (row <- table) yield (for (cell <- row) yield if (cell == null) 0 else cell.toString.length)
      val colSizes = for (col <- sizes.transpose) yield col.max
      val rows = for (row <- table) yield formatRow(row, colSizes)
      formatRows(rowSeparator(colSizes), rows)
  }

  def formatRows(rowSeparator: String, rows: Seq[String]): String = (
    rowSeparator ::
      rows.head ::
      rowSeparator ::
      rows.tail.toList :::
      rowSeparator ::
      List()).mkString("\n")

  def formatRow(row: Seq[Any], colSizes: Seq[Int]) = {
    val cells = (for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else ("%" + size + "s").format(item))
    cells.mkString("|", "|", "|")
  }

  def rowSeparator(colSizes: Seq[Int]) = colSizes map { "-" * _ } mkString("+", "+", "+")
}