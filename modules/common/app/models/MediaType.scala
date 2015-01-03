package models

import play.api.libs.json.{JsObject, Json}
import reactivemongo.bson.BSONObjectID

case class MediaType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel[String] {
  override def toString = List(mediaType, mediaSubtype, mime).mkString(":")

  override def equals(other: Any) = other match {
    case that: MediaType =>
      that.mime == this.mime &&
      that.mediaType == this.mediaType &&
      that.mediaSubtype == this.mediaSubtype
    case _ => false
  }

  override def hashCode: Int = (mediaType + mediaSubtype + mime).hashCode

  override def toJson: JsObject = Json.obj(
    "mime" -> mime,
    "type" ->  mediaType,
    "subtype" -> mediaSubtype
  )
  override protected def genId: String = BSONObjectID.generate.toString()
}
