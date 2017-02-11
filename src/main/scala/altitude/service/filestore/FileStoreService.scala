package altitude.service.filestore

import altitude.Context
import altitude.models.{Asset, Folder}
import altitude.transactions.TransactionId

trait FileStoreService {
  def calculateAssetPath(asset: Asset)
                        (implicit ctx: Context, txId: TransactionId = new TransactionId): String

  def addAsset(asset: Asset)(implicit ctx: Context)

  def deleteAsset(asset: Asset)(implicit ctx: Context)

  def moveAsset(asset: Asset, folder: Folder)(implicit ctx: Context)

  def addFolder(folder: Folder)(implicit ctx: Context)

  def deleteFolder(folder: Folder)(implicit ctx: Context)

  def moveFolder(folder: Folder, newName: String)(implicit ctx: Context)

  def calculateFolderPath(name: String, parentId: String)
                         (implicit ctx: Context, txId: TransactionId = new TransactionId): String

  def sortedFolderPath(implicit ctx: Context): String

  def triageFolderPath(implicit ctx: Context): String
}
