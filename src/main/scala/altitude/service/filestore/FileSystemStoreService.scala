package altitude.service.filestore

import altitude.{Altitude, Context}
import altitude.models.{Asset, Folder}
import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory

class FileSystemStoreService(app: Altitude) extends FileStoreService {
  private final val log = LoggerFactory.getLogger(getClass)

  override def createFolder(parent: Folder, folder: Folder)(implicit ctx: Context, txId: TransactionId): Unit = {

  }

  override def deleteFolder(id: String)(implicit ctx: Context, txId: TransactionId): Unit = {

  }

  override def addAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Adding asset $asset")
  }

  override def deleteAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {

  }

  override def moveAsset(asset: Asset, folder: Folder)(implicit ctx: Context, txId: TransactionId): Unit = {

  }
}
