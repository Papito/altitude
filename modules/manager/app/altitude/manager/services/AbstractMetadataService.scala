package altitude.manager.services

import altitude.common.models.{MediaType, Metadata}
import altitude.manager.models.FileImportAsset

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata
}
