package models

import play.api.libs.json.{Json, JsObject}
import reactivemongo.bson.BSONObjectID

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel[String] {
  override def toJson: JsObject = Json.obj()
  override protected def genId: String = BSONObjectID.generate.toString()
}