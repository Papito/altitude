package altitude.models

import play.api.libs.json.{JsObject, Json}
import scala.language.implicitConversions

object MediaType {
  implicit def toJson(obj: MediaType) = Json.obj(
    "mime" -> obj.mime,
    "type" ->  obj.mediaType,
    "subtype" -> obj.mediaSubtype)

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
