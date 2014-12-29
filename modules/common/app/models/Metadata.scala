package models

import play.api.libs.json.Json

object Metadata {
  implicit val jsonFormat = Json.format[Metadata]
}

case class Metadata(private val raw: Map[String, String]) extends BaseModel {
  override def toMap = raw
}
