package software.altitude.core.models

import play.api.libs.json.{JsObject, JsValue, Json, JsonConfiguration, OFormat, OWrites, Reads}
import play.api.libs.json.JsonNaming.SnakeCase

import scala.language.implicitConversions

object SystemMetadata {
  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit val format: OFormat[SystemMetadata] = Json.format[SystemMetadata]

  implicit def fromJson(json: JsValue): SystemMetadata = Json.fromJson[SystemMetadata](json).get
}

case class SystemMetadata(version: Int, isInitialized: Boolean) {
  val toJson: JsObject = Json.toJson(this).as[JsObject]

  override def toString: String = s"<system> version=$version, initialized=$isInitialized"
}
