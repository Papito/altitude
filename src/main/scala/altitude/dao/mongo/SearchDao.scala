package altitude.dao.mongo

import altitude.transactions.TransactionId
import altitude.{Context, Altitude}
import altitude.models.{MetadataField, Asset}

class SearchDao(val app: Altitude) extends BaseMongoDao("search_tokens") with altitude.dao.SearchDao {

  override def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])
                         (implicit ctx: Context, txId: TransactionId) = {

  }
}
