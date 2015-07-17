package altitude.dao

import altitude.models.{Asset, Preview}
import altitude.transactions.TransactionId

//FIXME: AssetDao
trait LibraryDao extends BaseDao {
  //FIXME: return preview object
  def addPreview(asset: Asset, bytes: Array[Byte])(implicit txId: TransactionId = new TransactionId): Option[String]
  //FIXME: should not return Option - 404 will be thrown when getting by ID
  def getPreview(id: String)(implicit txId: TransactionId = new TransactionId): Option[Preview]
}
