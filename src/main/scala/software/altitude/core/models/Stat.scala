package software.altitude.core.models

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._

import scala.language.implicitConversions


object Stat {
  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit val writes: OWrites[Stat] = Json.writes[Stat]
  implicit val reads: Reads[Stat] = Json.reads[Stat]
  implicit def fromJson(json: JsValue): Stat = Json.fromJson[Stat](json).get
  implicit def toJson(stats: Stat): JsObject = stats.toJson
}

case class Stat(dimension: String, dimVal: Int) {
  val toJson: JsObject = Json.toJson(this).as[JsObject]
}
