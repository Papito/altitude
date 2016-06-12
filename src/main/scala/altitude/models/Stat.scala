package altitude.models

import altitude.{Const => C}
import play.api.libs.json._
import scala.language.implicitConversions


object Stat {
  val DIMENSIONS = Set(
    C("Stats.TOTAL_ASSETS"),
    C("Stats.TOTAL_BYTES"),
    C("Stats.UNCATEGORIZED_ASSETS"),
    C("Stats.RECYCLED_ASSETS"),
    C("Stats.RECYCLED_BYTES"))

  implicit def fromJson(json: JsValue): Stat = {
    val statsMap = json.as[Map[String, Int]]
    Stat(
      statsMap.head._1,
      statsMap.head._2
    )
  }

  implicit def toJson(stats: Stat): JsObject = stats.toJson
}

case class Stat(dimension: String, dimVal: Int) {

  def toJson = {
    Json.obj(dimension -> dimVal)
  }
}

