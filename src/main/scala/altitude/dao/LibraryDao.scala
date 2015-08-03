package altitude.dao

import altitude.models.{Asset, Preview}
import altitude.transactions.TransactionId

//FIXME: AssetDao and PreviewDao
trait LibraryDao extends BaseDao {
  def addPreview(asset: Asset, bytes: Array[Byte])(implicit txId: TransactionId = new TransactionId): Option[Preview]
  def getPreview(id: String)(implicit txId: TransactionId = new TransactionId): Option[Preview]
}
