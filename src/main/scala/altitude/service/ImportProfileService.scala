package altitude.service

import altitude.Altitude
import altitude.dao.{ImportProfileDao, AssetDao}
import altitude.models.{ImportProfile, Asset}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class ImportProfileService(app: Altitude) extends BaseService[ImportProfile](app) {
  val log =  LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[ImportProfileDao]
}
