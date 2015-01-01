package models

import play.api.libs.json.{Json, JsObject}
import reactivemongo.bson.BSONObjectID

case class Metadata(private val raw: Map[String, String]) extends BaseModel[String] {
  override def toJson: JsObject = Json.obj()
  override protected def genId: String = BSONObjectID.generate.toString()
}