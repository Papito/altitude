package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}
import altitude.{Const => C}

import scala.language.implicitConversions

object MediaType {
  implicit def fromJson(json: JsValue): MediaType = try {
    new MediaType(
      mediaType = (json \ C.Asset.MEDIA_TYPE).as[String],
      mediaSubtype = (json \ C.Asset.MEDIA_SUBTYPE).as[String],
      mime = (json \ C.Asset.MIME_TYPE).as[String])
  } catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert from $json: ${e.getMessage}")
  }
}

case class MediaType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel {
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
