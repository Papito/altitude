package altitude.service

import altitude.dao.SearchDao
import altitude.models.{Asset, MetadataField}
import altitude.transactions.TransactionId
import altitude.util.QueryResult
import altitude.{Altitude, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class SearchService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val SEARCH_DAO = app.injector.instance[SearchDao]


  def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId) = {
    require(asset.path.isEmpty)
    log.info(s"Indexing asset $asset")
    val metadataFields: Map[String, MetadataField] = app.service.metadata.getAllFields
    SEARCH_DAO.indexAsset(asset, metadataFields)
  }

  def search(textQuery: String)(implicit ctx: Context, txId: TransactionId): QueryResult = {
    SEARCH_DAO.search(textQuery)
  }
}
