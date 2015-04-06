package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}

import scala.language.implicitConversions

object MediaType {
  implicit def toJson(obj: MediaType) = Json.obj(
    "mime" -> obj.mime,
    "type" ->  obj.mediaType,
    "subtype" -> obj.mediaSubtype)

  implicit def fromJson(json: JsValue): MediaType = try {
    new MediaType(
      mediaType = (json \ "type").as[String],
      mediaSubtype = (json \ "subtype").as[String],
      mime = (json \ "mime").as[String])
  } catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert to Asset from $json")
  }
}

case class MediaType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel {
  override def toString = List(mediaType, mediaSubtype, mime).mkString(":")

  override def equals(other: Any) = other match {
    case that: MediaType =>
      that.mime == this.mime &&
      that.mediaType == this.mediaType &&
      that.mediaSubtype == this.mediaSubtype
    case _ => false
  }

  override def hashCode: Int = (mediaType + mediaSubtype + mime).hashCode
}
