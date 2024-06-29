package software.altitude.core.service.filestore

import software.altitude.core.models.Asset
import software.altitude.core.models.Data
import software.altitude.core.models.Preview

trait FileStoreService {
  val pathSeparator: String

  def getById(id: String): Data

  def createPath(relPath: String): Unit

  def addAsset(asset: Asset): Unit

  def getFolderPath(name: String, parentId: String)
                         : String

  def getAssetPath(asset: Asset): String

  def sortedFolderPath: String

  def addPreview(preview: Preview): Unit

  def getPreviewById(assetId: String): Preview

  def assemblePath(pathComponents: List[String]): String
}
