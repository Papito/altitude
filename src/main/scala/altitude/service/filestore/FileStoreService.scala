package altitude.service.filestore

import altitude.Context
import altitude.models.{Asset, Folder}
import altitude.transactions.TransactionId

trait FileStoreService {
  def addAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)
  def deleteAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)
  def moveAsset(asset: Asset, folder: Folder)(implicit ctx: Context, txId: TransactionId = new TransactionId)
  def createFolder(parent: Folder, folder: Folder)(implicit ctx: Context, txId: TransactionId = new TransactionId)
  def deleteFolder(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId)
}
