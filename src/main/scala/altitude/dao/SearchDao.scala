package altitude.dao

import altitude.Context
import altitude.models.Asset
import altitude.transactions.TransactionId

trait SearchDao extends BaseDao {
  def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId)
}
