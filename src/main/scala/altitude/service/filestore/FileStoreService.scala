package altitude.service.filestore

import altitude.Context
import altitude.models.Folder
import altitude.transactions.TransactionId

trait FileStoreService {
  def createFolder(folder: Folder)(implicit ctx: Context, txId: TransactionId = new TransactionId)
  def deleteFolder(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId)
}
