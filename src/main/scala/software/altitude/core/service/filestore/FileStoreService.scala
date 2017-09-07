package software.altitude.core.service.filestore

import software.altitude.core.Context
import software.altitude.core.models.{Asset, Data, Folder, Preview}
import software.altitude.core.transactions.TransactionId

trait FileStoreService {
  val pathSeparator: String

  def getById(id: String)
             (implicit ctx: Context, txId: TransactionId = new TransactionId): Data

  def createPath(relPath: String)(implicit ctx: Context)

  def addAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)

  def purgeAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)

  def moveAsset(srcAsset: Asset, destAsset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)

  def recycleAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)

  def restoreAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)

  def addFolder(folder: Folder)(implicit ctx: Context)

  def deleteFolder(folder: Folder)(implicit ctx: Context)

  def renameFolder(folder: Folder, newName: String)(implicit ctx: Context)

  def moveFolder(folder: Folder, newParent: Folder)(implicit ctx: Context)

  def getFolderPath(name: String, parentId: String)
                         (implicit ctx: Context, txId: TransactionId = new TransactionId): String

  def calculateNextAvailableFilename(asset: Asset)(implicit ctx: Context, txId: TransactionId): String

  def getPathWithNewFilename(asset: Asset, newFilename: String)
                            (implicit ctx: Context, txId: TransactionId = new TransactionId): String

  def getAssetPath(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId): String

  def getRecycledAssetPath(asset: Asset)(implicit ctx: Context): String

  def sortedFolderPath(implicit ctx: Context): String

  def triageFolderPath(implicit ctx: Context): String

  def trashFolderPath(implicit ctx: Context): String

  def landfillFolderPath(implicit ctx: Context): String

  def addPreview(preview: Preview)(implicit ctx: Context)

  def getPreviewById(assetId: String)(implicit ctx: Context): Preview

  def assemblePath(pathComponents: List[String]): String
}
