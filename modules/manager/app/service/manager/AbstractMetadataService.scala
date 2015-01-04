package service.manager

import models.common.{Metadata, MediaType}
import models.manager.FileImportAsset

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata
}
