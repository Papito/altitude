package models.common

import play.api.libs.json.{JsObject, Json}
import reactivemongo.bson.BSONObjectID

case class Metadata(private val raw: Map[String, String]) extends BaseModel[String] {
  override def toJson: JsObject = Json.obj(
    "id" -> id
  )
  override protected def genId: String = BSONObjectID.generate.toString()
}