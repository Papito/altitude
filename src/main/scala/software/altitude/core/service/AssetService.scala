package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
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
  override protected val DAO = app.injector.instance[AssetDao]

  def setAssetAsRecycled(assetId: String, isRecycled: Boolean)(implicit ctx: Context, txId: TransactionId) = {
    DAO.setAssetAsRecycled(assetId, isRecycled = isRecycled)
  }

  override def query(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult = {
    DAO.queryNotRecycled(q)
  }

  def queryRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult = {
    DAO.queryRecycled(q)
  }

  // NO
  // Read the class description
}
