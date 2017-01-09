package altitude.service

import altitude.transactions.TransactionId
import altitude.{Context, Altitude}
import altitude.dao.NotImplementedDao
import altitude.models.Asset

class SearchService(val app: Altitude) extends BaseService {
  override protected val DAO = new NotImplementedDao(app)

  def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId) = {

  }
}
