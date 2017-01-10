package altitude.service.search

import altitude.models.Asset
import altitude.transactions.TransactionId
import altitude.{Altitude, Context}
import org.slf4j.LoggerFactory

class SqlSearchService(app: Altitude) extends SearchService {
  private final val log = LoggerFactory.getLogger(getClass)

  def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId) = {

  }
}
