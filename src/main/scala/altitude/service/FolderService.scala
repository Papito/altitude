package altitude.service

import altitude.Altitude
import altitude.dao.FolderDao
import altitude.models.Folder
import org.slf4j.LoggerFactory

import net.codingwell.scalaguice.InjectorExtensions._


class FolderService(app: Altitude) extends BaseService[Folder](app){
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[FolderDao]
}
