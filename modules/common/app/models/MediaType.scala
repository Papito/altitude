package models

import play.api.libs.json.Json

object MediaType {
  implicit val jsonFormat = Json.format[MediaType]
}

case class MediaType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel {
  override def toString = List(mediaType, mediaSubtype, mime).mkString(":")

  override def toMap = Map(
    "type" -> mediaType,
    "subtype" -> mediaSubtype,
    "mime" -> mime)

  override def equals(other: Any) = other match {
    case that: MediaType =>
      that.mime == this.mime &&
      that.mediaType == this.mediaType &&
      that.mediaSubtype == this.mediaSubtype
    case _ => false
  }

  override def hashCode: Int = (mediaType + mediaSubtype + mime).hashCode
}
