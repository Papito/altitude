package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import software.altitude.core.dao.SearchDao
import software.altitude.core.models.{Asset, MetadataField}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.QueryResult
import software.altitude.core.{Altitude, Context}

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