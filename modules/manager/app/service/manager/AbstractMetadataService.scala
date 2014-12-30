package service.manager

import models.manager.FileImportAsset
import models.{MediaType, Metadata}

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata
}
