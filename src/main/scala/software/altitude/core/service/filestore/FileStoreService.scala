package software.altitude.core.service.filestore

import software.altitude.core.models.{Asset, Face, MimedAssetData, MimedPreviewData}

trait FileStoreService {
  def addAsset(asset: Asset): Unit
  def getAssetById(id: String): MimedAssetData

  def addPreview(preview: MimedPreviewData): Unit
  def getPreviewById(assetId: String): MimedPreviewData

  def addFace(face: Face): Unit
}
