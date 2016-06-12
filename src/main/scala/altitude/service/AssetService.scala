package altitude.service

import altitude.Altitude
import altitude.dao.AssetDao
import altitude.models.{Stats, Asset}
import altitude.models.search.Query
import altitude.transactions.TransactionId
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

class AssetService(app: Altitude) extends BaseService[Asset](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[AssetDao]

  override def add(asset: Asset, queryForDup: Option[Query] = None)(implicit txId: TransactionId = new TransactionId): JsObject = {
    txManager.withTransaction[JsObject] {
      app.service.stats.incrementStat(Stats.TOTAL_ASSETS)
      super.add(asset, queryForDup)
    }
  }
}
