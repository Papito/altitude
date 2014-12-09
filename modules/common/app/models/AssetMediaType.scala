package models

class AssetMediaType(mediaType: String, mediaSubtype: String, mime: String) {
  override def toString = List(mediaType, mediaSubtype, mime).mkString(":")
}
