package altitude.models

import play.api.libs.json.{JsObject, Json}
import reactivemongo.bson.BSONObjectID

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel {
  override def toJson: JsObject = Json.obj(
    "id" -> id,
    "mediaType" -> mediaType.toJson
  )
}