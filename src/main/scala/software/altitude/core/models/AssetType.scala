package software.altitude.core.models

import play.api.libs.json.{JsObject, JsValue, Json, JsonConfiguration, OFormat, OWrites, Reads}
import play.api.libs.json.JsonNaming.SnakeCase

import scala.language.implicitConversions

object AssetType {
  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit val format: OFormat[AssetType] = Json.format[AssetType]
  implicit def fromJson(json: JsValue): AssetType = Json.fromJson[AssetType](json).get
}

case class AssetType(mediaType: String,
                     mediaSubtype: String,
                     mime: String
                    ) extends BaseModel with NoId with NoDates {

  val toJson: JsObject = Json.toJson(this).as[JsObject]

  override def equals(other: Any): Boolean = other match {
    case that: AssetType =>
      that.mime == this.mime &&
      that.mediaType == this.mediaType &&
      that.mediaSubtype == this.mediaSubtype
    case _ => false
  }

  override def toString: String = List(mediaType, mediaSubtype, mime).mkString(":")

  override def hashCode: Int = (mediaType + mediaSubtype + mime).hashCode
}
