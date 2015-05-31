package altitude.service

import altitude.models.{FileImportAsset, MediaType}
import play.api.libs.json.JsValue

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): JsValue
}
