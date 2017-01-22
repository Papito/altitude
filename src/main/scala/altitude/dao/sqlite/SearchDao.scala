package altitude.dao.sqlite

import altitude.transactions.TransactionId
import altitude.util.QueryResult
import altitude.{Const => C, Context, Altitude}
import altitude.models.Asset
import org.slf4j.LoggerFactory

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Sqlite {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected def addSearchDocument(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
  }

  override def search(textQuery: String)
                     (implicit ctx: Context, txId: TransactionId): QueryResult = {
  
    QueryResult(records = List(), total = 0, None)
  }

}