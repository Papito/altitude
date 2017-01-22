package altitude.dao

import altitude.Context
import altitude.models.{Asset, MetadataField}
import altitude.transactions.TransactionId
import altitude.util.QueryResult

trait SearchDao {
  def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])
                (implicit ctx: Context, txId: TransactionId): Unit
  def search(textQuery: String)(implicit ctx: Context, txId: TransactionId): QueryResult
}
