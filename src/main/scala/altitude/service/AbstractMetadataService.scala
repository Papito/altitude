package altitude.service

import java.io.InputStream

import altitude.models.{FileImportAsset, MediaType}
import play.api.libs.json.JsValue

abstract class AbstractMetadataService {
  val FIELD_BIBLE: Map[String, List[String]] = Map(
    "X Resolution" -> List("X Resolution"),
    "Y Resolution" -> List("Y Resolution")
  )

  def extract(importAsset: FileImportAsset, mediaType: MediaType): JsValue
  def detectMediaTypeFromStream(is: InputStream): MediaType
}
