package altitude.dao

import altitude.models.{Preview, Asset}
import altitude.transactions.TransactionId

trait LibraryDao extends BaseDao {
  //FIXME: Rename to just "addPreview"
  def addImagePreview(asset: Asset, bytes: Array[Byte]): Option[String]
  def getPreview(id: String)(implicit txId: TransactionId = new TransactionId): Option[Preview]
}
