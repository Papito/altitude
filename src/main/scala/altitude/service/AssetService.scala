package altitude.service

import altitude.dao.AssetDao
import altitude.models.Asset
import altitude.Altitude
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class AssetService(app: Altitude) extends BaseService[Asset](app) {
  val log =  LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[AssetDao]
}
