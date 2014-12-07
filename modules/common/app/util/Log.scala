package util

import play.api.libs.json.{JsObject, JsString}
import play.api.libs.json.Json
import java.util.logging.Level

object log {
  private val MSG:   String = "msg"
  private val LEVEL: String = "level"
  private val TAGS:  String = "tags"

  private val LOGGER_TAG = "LOGGER"

  def debug(msg: String, v: Map[String, String], t: String*) =
    log(Level.FINE, msg = msg, v = v, t = t)

  def debug(msg: String, t: String*) =
    log(Level.FINE, msg = msg, t = t)


  def info(msg: String, v: Map[String, String], t: String*) =
    log(Level.INFO, msg = msg, v = v, t = t)

  def info(msg: String, t: String*) =
    log(Level.INFO, msg = msg, t = t)


  def warn(msg: String, v: Map[String, String], t: String*) =
    log(Level.WARNING, msg = msg, v = v, t = t)

  def warn(msg: String, t: String*) =
    log(Level.WARNING, msg = msg, t = t)


  def error(msg: String, v: Map[String, String], t: String*) =
    log(Level.SEVERE, msg = msg, v = v, t = t)

  def error(msg: String, t: String*) = log(Level.SEVERE, msg = msg, t = t)

  private def log(level: Level, msg: String,
                  v: Map[String, String] = Map(),
                  t: Seq[String] = Seq()): Unit = {
    val stm = this.getLogStmt(level = level, msg = msg, v = v, t = t)
    this.output(stm)
  }

  protected def output(stm: String): Unit = {
    println(stm)
  }

  def getLogStmt(level: Level, msg: String,
                 v: Map[String, String] = Map(),
                 t: Seq[String] = Seq()) = {
    val values = v map { case (key, value) => (key, JsString(value))}

    // check if arbitrary value keys collide with reserved ones
    for (reservedKey: String <- List(MSG, LEVEL, TAGS)) {
      if (v.contains(reservedKey)) {
        warn(
          "Logger reserved key collision for '$key'. Lost value: '$value'",
          Map[String, String]("key" -> reservedKey, "value" -> v(reservedKey)),
          LOGGER_TAG
        )
      }
    }

    // replace all occurrences of variables in the message
    val interpolatedMsg = v.foldLeft(msg){
      case (out, (k, v)) => out.replaceAll("\\$"+k, v)
    }

    val json = Json.obj(
      MSG   -> interpolatedMsg,
      LEVEL -> level.toString,
      TAGS  -> t
    ) ++ JsObject(values.toSeq)

    Json.stringify(json)
  }
}
