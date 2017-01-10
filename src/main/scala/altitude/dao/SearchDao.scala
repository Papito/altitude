package altitude.dao

import altitude.Context
import altitude.models.{MetadataField, Asset}
import altitude.transactions.TransactionId

trait SearchDao {
  def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])(implicit ctx: Context, txId: TransactionId)
}
