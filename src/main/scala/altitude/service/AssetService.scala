package altitude.service

import altitude.Altitude
import altitude.dao.AssetDao
import altitude.models.Asset
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class AssetService(app: Altitude) extends BaseService[Asset](app) {
  /*
  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

  This is a "dumb" service. If there is anything special to be done with an asset, it's under
  the jurisdiction of the Library service - it does all the counter decrementin' and wrist slappin'
   */

  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[AssetDao]

}
