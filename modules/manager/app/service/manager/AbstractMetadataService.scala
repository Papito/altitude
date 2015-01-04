package service.manager

import models.common.{MediaType, Metadata}
import models.manager.FileImportAsset

abstract class AbstractMetadataService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata
}
