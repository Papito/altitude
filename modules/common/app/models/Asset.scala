package models

class Asset(val mediaType: MediaType, val metadata: Metadata) extends BaseModel {
  override def toString = ""
  override def toMap = Map(
    "mediaType" -> mediaType.toMap)
}
