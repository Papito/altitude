package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsValue, Json}

import scala.language.implicitConversions

object AssetType {
  implicit def fromJson(json: JsValue): AssetType = new AssetType(
      mediaType = (json \ C.AssetType.MEDIA_TYPE).as[String],
      mediaSubtype = (json \ C.AssetType.MEDIA_SUBTYPE).as[String],
      mime = (json \ C.AssetType.MIME_TYPE).as[String])
}

case class AssetType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel with NoId {
  override def toString = List(mediaType, mediaSubtype, mime).mkString(":")

  override val toJson =  Json.obj(
    C.AssetType.MIME_TYPE -> mime,
    C.AssetType.MEDIA_TYPE ->  mediaType,
    C.AssetType.MEDIA_SUBTYPE -> mediaSubtype
  )

  override def equals(other: Any) = other match {
    case that: AssetType =>
      that.mime == this.mime &&
      that.mediaType == this.mediaType &&
      that.mediaSubtype == this.mediaSubtype
    case _ => false
  }

  override def hashCode: Int = (mediaType + mediaSubtype + mime).hashCode
}
