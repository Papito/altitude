package service.manager

import models.{MediaType, Metadata}
import models.manager.FileImportAsset
import constants.{const => C}
import util.log

class MetadataService extends BaseService {
  def extract(importAsset: FileImportAsset, mediaType: MediaType): Metadata = {
    log.info("Extracting metadata for $asset", Map("asset" -> importAsset), C.tag.SERVICE)

    mediaType match {
      case mt: MediaType if mt.mediaType == "image" => extractImageMetadata(importAsset)
      case mt: MediaType if mt.mediaType == "audio" => extractAudioMetadata(importAsset)
      case _ => {
        log.warn(
          "No metadata extractor found for $asset of type '$mediaType'",
          Map("asset" -> importAsset, "mediaType" -> mediaType.mediaType),
          C.tag.SERVICE)
        null
      }
    }
  }

  private def extractImageMetadata(importAsset: FileImportAsset): Metadata = {
    log.info("Extracting IMAGE medadata for $asset", Map("asset" -> importAsset), C.tag.SERVICE)
    new Metadata
  }

  private def extractAudioMetadata(importAsset: FileImportAsset): Metadata = {
    log.info("Extracting AUDIO medadata for $asset", Map("asset" -> importAsset), C.tag.SERVICE)
    new Metadata
  }}
