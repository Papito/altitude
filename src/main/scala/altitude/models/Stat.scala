package altitude.models

import altitude.{Const => C}
import play.api.libs.json._
import scala.language.implicitConversions


object Stat {
  implicit def fromJson(json: JsValue): Stat = Stat(
      (json \ C("Stat.DIMENSION")).as[String],
      (json \ C("Stat.DIM_VAL")).as[Int]
  )

  implicit def toJson(stats: Stat): JsObject = stats.toJson
}

case class Stat(dimension: String, dimVal: Int) {

  def toJson = {
    Json.obj(
      C("Stat.DIMENSION") -> dimension,
      C("Stat.DIM_VAL") -> dimVal)
  }
}
