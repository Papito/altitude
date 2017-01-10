package altitude.service

import altitude.dao.{MetadataFieldDao, SearchDao}
import altitude.models.{MetadataField, Asset}
import altitude.transactions.TransactionId
import altitude.{Altitude, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class SearchService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val SEARCH_DAO = app.injector.instance[SearchDao]


  def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId) = {
    log.info(s"Indexing asset $asset")
    val metadataFields: Map[String, MetadataField] = app.service.metadata.getAllFields
    SEARCH_DAO.indexAsset(asset, metadataFields)
  }
}
