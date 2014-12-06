package util

import play.api.libs.json.{JsObject, JsString}
import play.api.libs.json.Json
import java.util.logging.Level

object log {

  def debug(msg: String, v: Map[String, String], t: String*): Unit = {
    this.log(Level.FINE, msg = msg, v = v, t = t)
  }

  def debug(msg: String, t: String*): Unit = {
    this.log(Level.FINE, msg = msg, t = t)
  }

  def info(msg: String, v: Map[String, String], t: String*): Unit = {
    this.log(Level.INFO, msg = msg, v = v, t = t)
  }

  def info(msg: String, t: String*): Unit = {
    this.log(Level.INFO, msg = msg, t = t)
  }

  private def log(level: Level, msg: String,
                  v: Map[String, String] = Map[String, String](),
                  t: Seq[String] = Seq[String]()): Unit = {
    val stm = this.getLogStmt(level = level, msg = msg, v = v, t = t)
    this.output(stm)
  }

  protected def output(stm: String): Unit = {
    println(stm)
  }

  def getLogStmt(level: Level, msg: String,
                 v: Map[String, String] = Map[String, String](),
                 t: Seq[String] = Seq[String]()) = {
    val values = v map { case (key, value) => (key, JsString(value))}

    // replace all occurrences of variables in the message (start with '$')
    val interpolatedMsg = v.foldLeft(msg){
      case (out, (k, v)) => out.replaceAll("\\$"+k, v)
    }

    val json = Json.obj(
      "msg"   -> interpolatedMsg,
      "level" -> level.toString,
      "tags"  -> t
    ) ++ JsObject(values.toSeq)

    Json.stringify(json)
  }
}
