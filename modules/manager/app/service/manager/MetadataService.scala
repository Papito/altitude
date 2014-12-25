package service.manager

import models.{MediaType, Metadata}
import models.manager.FileImportAsset

class MetadataService extends BaseService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata = {
    new Metadata
  }
}
