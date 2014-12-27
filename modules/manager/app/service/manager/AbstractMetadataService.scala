package service.manager

import models.manager.FileImportAsset
import models.{MediaType, Metadata}

abstract class AbstractMetadataService extends BaseService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata
}
