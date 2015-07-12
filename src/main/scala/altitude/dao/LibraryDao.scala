package altitude.dao

import altitude.models.{Asset, Preview}
import altitude.transactions.TransactionId

trait LibraryDao extends BaseDao {
  def addPreview(asset: Asset, bytes: Array[Byte]): Option[String]
  def getPreview(id: String)(implicit txId: TransactionId = new TransactionId): Option[Preview]
}
