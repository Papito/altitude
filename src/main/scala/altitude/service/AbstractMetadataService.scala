package altitude.service

import java.io.InputStream

import altitude.models.{FileImportAsset, MediaType}
import play.api.libs.json.JsValue

abstract class AbstractMetadataService {
  val FIELD_BIBLE = Map(
    "X Resolution" -> Set("X Resolution"),
    "Y Resolution" -> Set("Y Resolution")
  )

  def extract(importAsset: FileImportAsset, mediaType: MediaType): JsValue
  def detectMediaTypeFromStream(is: InputStream): MediaType
}
