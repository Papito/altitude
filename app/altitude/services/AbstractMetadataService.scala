package altitude.services

import altitude.models.{Metadata, MediaType, FileImportAsset}

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata
}
