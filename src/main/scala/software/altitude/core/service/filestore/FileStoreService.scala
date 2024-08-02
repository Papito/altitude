package software.altitude.core.service.filestore

import software.altitude.core.models.Asset
import software.altitude.core.models.MimedAssetData
import software.altitude.core.models.MimedPreviewData

trait FileStoreService {
  def addAsset(asset: Asset): Unit
  def getAssetById(id: String): MimedAssetData

  def addPreview(preview: MimedPreviewData): Unit
  def getPreviewById(assetId: String): MimedPreviewData
}
