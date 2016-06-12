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
  /*
  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

  This is a "dumb" service. If there is anything special to be done with an asset, it's under
  the jurisdiction of the Library service - it does all the counter decrementin' and wrist slappin'
   */

  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[AssetDao]

}
