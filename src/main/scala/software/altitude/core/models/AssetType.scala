package software.altitude.core.models

import play.api.libs.json.{JsObject, JsValue, Json}
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object AssetType {
  implicit def fromJson(json: JsValue): AssetType = new AssetType(
      mediaType = (json \ C.AssetType.MEDIA_TYPE).as[String],
      mediaSubtype = (json \ C.AssetType.MEDIA_SUBTYPE).as[String],
      mime = (json \ C.AssetType.MIME_TYPE).as[String])
}

case class AssetType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel with NoId {
  override def toString: String = List(mediaType, mediaSubtype, mime).mkString(":")

  override val toJson: JsObject =  Json.obj(
    C.AssetType.MIME_TYPE -> mime,
    C.AssetType.MEDIA_TYPE ->  mediaType,
    C.AssetType.MEDIA_SUBTYPE -> mediaSubtype
  )

  override def equals(other: Any): Boolean = other match {
    case that: AssetType =>
      that.mime == this.mime &&
      that.mediaType == this.mediaType &&
      that.mediaSubtype == this.mediaSubtype
    case _ => false
  }

  override def hashCode: Int = (mediaType + mediaSubtype + mime).hashCode
}
