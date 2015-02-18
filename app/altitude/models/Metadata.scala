package altitude.models

import play.api.libs.json.{JsObject, Json}

case class Metadata(private val raw: Map[String, String]) extends BaseModel {
  override def toJson: JsObject = Json.obj(
    "id" -> id
  )
}