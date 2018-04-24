package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import software.altitude.core.dao.AssetDao
import software.altitude.core.models.Asset
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.{Query, QueryResult}
import software.altitude.core.{Altitude, Context}

/**
 * This is a "dumb" service - meaning it delegates everything to to the base service implementation
 * and the base DAO. It does the basics, but shall not do anything more than that.
 *
 * If there is anything special to be done with an asset, it's under
 * the jurisdiction of the Library service - it does all the counter decrementin' and wrist slappin'
 */
class AssetService(val app: Altitude) extends BaseService[Asset] {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val dao: AssetDao = app.injector.instance[AssetDao]

  def setAssetAsRecycled(assetId: String, isRecycled: Boolean)(implicit ctx: Context, txId: TransactionId): Unit = {
    log.info(s"Setting asset [$assetId] recycled flag to [$isRecycled]")
    dao.setAssetAsRecycled(assetId, isRecycled = isRecycled)
  }

  override def query(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult = {
    dao.queryNotRecycled(q)
  }

  def queryRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult = {
    dao.queryRecycled(q)
  }

  // NO
  // Read the class description
}
