package service.manager

import models.{Metadata, MediaType}
import models.manager.FileImportAsset

abstract class MetadataService extends BaseService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata
}
