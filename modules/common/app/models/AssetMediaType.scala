package models

class AssetMediaType(mediaType: String, mediaSubtype: String, mime: String) extends BaseModel {
  override def toString = List(mediaType, mediaSubtype, mime).mkString(":")

  override def toDict = Map(
    "type" -> mediaType,
    "subtype" -> mediaSubtype,
    "mime" -> mime
  )
}
