package software.altitude.core.dao

import software.altitude.core.Context
import software.altitude.core.models.{Asset, MetadataField}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.{Query, QueryResult}

trait SearchDao {
  def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])
                (implicit ctx: Context, txId: TransactionId): Unit
  def search(query: Query)(implicit ctx: Context, txId: TransactionId): QueryResult
}
