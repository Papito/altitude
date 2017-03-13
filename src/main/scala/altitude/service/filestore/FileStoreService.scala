package altitude.service.filestore

import altitude.Context
import altitude.models.{Preview, Data, Asset, Folder}
import altitude.transactions.TransactionId

trait FileStoreService {
  def getById(id: String)
             (implicit ctx: Context, txId: TransactionId = new TransactionId): Data

  def addAsset(asset: Asset)(implicit ctx: Context)

  def purgeAsset(asset: Asset)(implicit ctx: Context)

  def moveAsset(asset: Asset, destPath: String)(implicit ctx: Context)

  def recycleAsset(asset: Asset)(implicit ctx: Context)

  def restoreAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)

  def addFolder(folder: Folder)(implicit ctx: Context)

  def deleteFolder(folder: Folder)(implicit ctx: Context)

  def moveFolder(folder: Folder, newName: String)(implicit ctx: Context)

  def calculateFolderPath(name: String, parentId: String)
                         (implicit ctx: Context, txId: TransactionId = new TransactionId): String

  def calculateAssetPath(asset: Asset, folder:Folder)
                         (implicit ctx: Context, txId: TransactionId = new TransactionId): String

  def calculateAssetPath(asset: Asset)(implicit ctx: Context): String

  def calculatePathWithNewFilename(asset: Asset, newFilename: String)(implicit ctx: Context): String

  def getAssetPath(asset: Asset)(implicit ctx: Context): String

  def getRecycledAssetPath(asset: Asset)(implicit ctx: Context): String

  def sortedFolderPath(implicit ctx: Context): String

  def triageFolderPath(implicit ctx: Context): String

  def trashFolderPath(implicit ctx: Context): String

  def landfillFolderPath(implicit ctx: Context): String

  def addPreview(preview: Preview)(implicit ctx: Context)

  def getPreviewById(assetId: String)(implicit ctx: Context): Preview
}
