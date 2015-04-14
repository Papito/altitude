package altitude.models

import play.api.libs.json.{JsObject, JsValue, Json}
import altitude.{Const => C}

import scala.language.implicitConversions

object MediaType {
  implicit def fromJson(json: JsValue): MediaType = MediaType(
    mediaType = (json \ C.Asset.MEDIA_TYPE).as[String],
    mediaSubtype = (json \ C.Asset.MEDIA_SUBTYPE).as[String],
    mime = (json \ C.Asset.MIME_TYPE).as[String]
  )
}

case class MediaType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel with NoId {
  override def toString = List(mediaType, mediaSubtype, mime).mkString(":")

  override def toJson =  Json.obj(
    C.Asset.MIME_TYPE -> mime,
    C.Asset.MEDIA_TYPE ->  mediaType,
    C.Asset.MEDIA_SUBTYPE -> mediaSubtype
  )

  override def equals(other: Any) = other match {
    case that: MediaType =>
      that.mime == this.mime &&
      that.mediaType == this.mediaType &&
      that.mediaSubtype == this.mediaSubtype
    case _ => false
  }

  override def hashCode: Int = (mediaType + mediaSubtype + mime).hashCode
}
