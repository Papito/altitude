package software.altitude.core.dao

import software.altitude.core.Context
import software.altitude.core.models.{Asset, MetadataField}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.QueryResult

trait SearchDao {
  def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])
                (implicit ctx: Context, txId: TransactionId): Unit
  def search(textQuery: String)(implicit ctx: Context, txId: TransactionId): QueryResult
}