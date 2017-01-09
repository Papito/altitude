package altitude.service

import altitude.Altitude
import altitude.dao.AssetDao
import altitude.models.Asset
import net.codingwell.scalaguice.InjectorExtensions._

/**
 * This is a "dumb" service - meaning it delegates everything to to the base service implementation
 * and the base DAO. It does the basics, but shall not do anything more than that.
 *
 * If there is anything special to be done with an asset, it's under
 * the jurisdiction of the Library service - it does all the counter decrementin' and wrist slappin'
 */
class AssetService(val app: Altitude) extends BaseService[Asset] {
  override protected val DAO = app.injector.instance[AssetDao]

  // NO
  // Read the class description
}
