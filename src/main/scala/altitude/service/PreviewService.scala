package altitude.service

import altitude.Altitude
import altitude.dao.{PreviewDao, AssetDao}
import altitude.models.{Preview, Asset}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class PreviewService(app: Altitude) extends BaseService[Preview](app) {
  val log =  LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[PreviewDao]
}
