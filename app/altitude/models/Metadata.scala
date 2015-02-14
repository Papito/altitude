package altitude.models

import play.api.libs.json.{JsObject, Json}
import reactivemongo.bson.BSONObjectID

case class Metadata(private val raw: Map[String, String]) extends BaseModel {
  override def toJson: JsObject = Json.obj(
    "id" -> id
  )
}