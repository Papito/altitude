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
    println(stm)
  }

  def getLogStmt(level: Level, msg: String,
                 v: Map[String, String] = Map[String, String](),
                 t: Seq[String] = Seq[String]()) = {
    val values = v map { case (key, value) => (key, JsString(value))}
    val json = Json.obj(
      "msg"   -> msg,
      "level" -> level.toString,
      "tags"  -> t
    ) ++ JsObject(values.toSeq)

    Json.stringify(json)
  }
}
