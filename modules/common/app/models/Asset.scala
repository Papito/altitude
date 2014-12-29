package models

import play.api.libs.json.Json

object Asset {
  implicit val jsonFormat = Json.format[Asset]
}

case class Asset(mediaType: MediaType, metadata: Metadata) extends BaseModel {
  override def toString = toMap.toString()
  override def toMap = Map(
    "mediaType" -> mediaType.toMap,
    "metadata"  -> metadata.toMap)
}