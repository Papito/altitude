package altitude.service.filestore

import altitude.{Altitude, Context}
import altitude.models.Folder
import altitude.transactions.TransactionId

class FileSystemStoreService(app: Altitude) extends  FileStoreService {

  override def createFolder(folder: Folder)(implicit ctx: Context, txId: TransactionId): Unit = {

  }

  override def deleteFolder(id: String)(implicit ctx: Context, txId: TransactionId): Unit = {

  }
}
