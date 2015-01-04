package util

import java.util.logging.Level

import play.api.libs.json.{JsObject, JsString, Json}

object log {
  var TIME_FORMAT = "yyyy-MM-dd HH:mm:ss:S"

  private var constants = scala.collection.mutable.Map[String, JsString]()
  private val MSG   :String = "msg"
  private val LEVEL :String = "level"
  private val TAGS  :String = "tags"
  private val LOC   :String = "loc"
  private val TIME  :String = "time"

  // for anything generated by the logger (when bad things happen)
  private val LOGGER_TAG = "LOGGER"

  //FIXME: should be initted in constructor
  def addConstant(name: String, value: Any): Unit = {
    constants += (name -> JsString(value.toString))
  }

  // TODO: respect the actual level
  def trace(msg: String, v: Map[String, Any], t: String*) =
    log(Level.FINER, msg = msg, v = v, t = t)

  def trace(msg: String, t: String*) =
    log(Level.FINER, msg = msg, t = t)

  def debug(msg: String, v: Map[String, Any], t: String*) =
    log(Level.FINE, msg = msg, v = v, t = t)

  def debug(msg: String, t: String*) =
    log(Level.FINE, msg = msg, t = t)

  def info(msg: String, v: Map[String, Any], t: String*) =
    log(Level.INFO, msg = msg, v = v, t = t)

  def info(msg: String, t: String*) =
    log(Level.INFO, msg = msg, t = t)

  def warn(msg: String, v: Map[String, Any], t: String*) =
    log(Level.WARNING, msg = msg, v = v, t = t)

  def warn(msg: String, t: String*) =
    log(Level.WARNING, msg = msg, t = t)

  def error(msg: String, v: Map[String, Any], t: String*) =
    log(Level.SEVERE, msg = msg, v = v, t = t)

  def error(msg: String, t: String*) =
    log(Level.SEVERE, msg = msg, t = t)

  private def log(level: Level, msg: String,
                  v: Map[String, Any] = Map(),
                  t: Seq[String] = Seq()): Unit = {
    val stm = getLogStmt(level = level, msg = msg, v = v, t = t)
    output(stm)
  }

  protected def output(stm: String): Unit = {
    println(stm)
  }

  protected def getTimestamp: String = {
    val format = new java.text.SimpleDateFormat(TIME_FORMAT)
    format.format(new java.util.Date())
  }

  def getLogStmt(level: Level, msg: String,
                 v: Map[String, Any] = Map(),
                 t: Seq[String] = Seq()) = {
    val values = v map { case (key, value) => (key, JsString(value.toString))}

    // check if arbitrary value keys collide with reserved ones
    for (reservedKey: String <- List(MSG, LEVEL, TAGS, LOC, TIME)) {
      if (v.contains(reservedKey)) {
        warn(
          "Logger reserved key collision for '$key'. Lost value: '$value'",
          Map[String, Any]("key" -> reservedKey, "value" -> v(reservedKey)),
          LOGGER_TAG
        )
      }
    }

    // replace all occurrences of $variables in the message
    // FIXME: used the scala string interpolation
    val interpolatedMsg = v.foldLeft(msg){
      case (out, (k, v)) => out.replaceAll("\\$"+k, v.toString)
    }

    // FIXME: this is wrong. Needs to intelligently find the closest call to logger
    val ctx: StackTraceElement = Thread.currentThread().getStackTrace()(4)
    val location = List(ctx.getFileName, ctx.getLineNumber)

    val json = Json.obj(
      TIME  -> JsString(getTimestamp),
      MSG   -> interpolatedMsg,
      LEVEL -> level.toString,
      TAGS  -> t,
      LOC   -> location.mkString(":")
    ) ++ JsObject(values.toSeq) ++ JsObject(constants.toSeq)

    //FIXME: use org.json4s.native.Serialization._?
    Json.stringify(json)
  }
}