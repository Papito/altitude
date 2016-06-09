package altitude.service

import altitude.Altitude
import altitude.dao.TrashDao
import altitude.models.Trash
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._

class TrashService(app: Altitude) extends BaseService[Trash](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[TrashDao]
}
