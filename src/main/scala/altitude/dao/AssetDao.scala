package altitude.dao

import altitude.Context
import altitude.models.Metadata
import altitude.transactions.TransactionId

trait AssetDao extends BaseDao {
  def setMetadata(assetId: String, metadata: Metadata)(implicit ctx: Context, txId: TransactionId)
  def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata]
}