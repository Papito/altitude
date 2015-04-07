package altitude.services

import altitude.models.{FileImportAsset, MediaType, Metadata}

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Option[Metadata]
}
