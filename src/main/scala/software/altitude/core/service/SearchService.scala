package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import software.altitude.core.dao.SearchDao
import software.altitude.core.models.{Asset, MetadataField}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.{QueryResult, SearchQuery}
import software.altitude.core.{Altitude, Context}

class SearchService(val app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val searchDao: SearchDao = app.injector.instance[SearchDao]

  def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    require(asset.path.isEmpty)
    log.info(s"Indexing asset $asset")
    val metadataFields: Map[String, MetadataField] = app.service.metadata.getAllFields
    searchDao.indexAsset(asset, metadataFields)
  }

  def search(query: SearchQuery)(implicit ctx: Context, txId: TransactionId): QueryResult = {
    searchDao.search(query)
  }

  def addMetadataValue(asset: Asset, field: MetadataField, value: String)
                      (implicit ctx: Context, txId: TransactionId): Unit = {
    searchDao.addMetadataValue(asset, field, value)
  }

  def addMetadataValues(asset: Asset, field: MetadataField, values: Set[String])
                       (implicit ctx: Context, txId: TransactionId): Unit = {

    searchDao.addMetadataValues(asset, field, values)
  }

  def deleteMetadataValue(asset: Asset, valueId: String)
                         (implicit ctx: Context, txId: TransactionId): Unit = {
    searchDao.deleteMetadataValue(asset, valueId)
  }
}
