package util
import play.api.libs.json._
import java.util.logging.Level

object log {

  def info(msg: String, v: Map[String, String], t: String*): Unit = {
    this.log(Level.INFO, msg = msg, v = v, t = t)
  }

  def info(msg: String, t: String*): Unit = {
    this.log(Level.INFO, msg = msg, t = t)
  }

  private def log(level: Level, msg: String, v: Map[String, String] = Map[String, String](), t: Seq[String] = Seq[String]()): Unit = {
    val stm = this.getLogStmt(level = level, msg = msg, v = v, t = t)
    println(stm)
  }

  def getLogStmt(level: Level, msg: String, v: Map[String, String] = Map[String, String](), t: Seq[String] = Seq[String]()) = {
    val json: JsValue = JsObject(Seq(
      "msg" -> JsString(msg),
      "level" -> JsString(level.toString)
    ))

    Json.stringify(json)
  }
}
