package software.altitude.core.models

import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.OWrites
import play.api.libs.json.Reads

import scala.language.implicitConversions

object SystemMetadata {
  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit val writes: OWrites[SystemMetadata] = Json.writes[SystemMetadata]
  implicit val reads: Reads[SystemMetadata] = Json.reads[SystemMetadata]

  implicit def fromJson(json: JsValue): SystemMetadata = Json.fromJson[SystemMetadata](json).get
}

case class SystemMetadata(version: Int, isInitialized: Boolean) {
  val toJson: JsObject = Json.toJson(this).as[JsObject]

  override def toString: String = s"<system> version=$version, initialized=$isInitialized"
}
