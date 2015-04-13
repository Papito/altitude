package altitude.services

import altitude.models.{FileImportAsset, MediaType}
import play.api.libs.json.{JsObject, JsValue}

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): JsValue
}
