package altitude.service.search

import altitude.Context
import altitude.models.Asset
import altitude.transactions.TransactionId

trait  SearchService {
  def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId)
}
