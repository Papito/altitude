package altitude.service

import altitude.dao.MetadataFieldDao
import altitude.models.Folder
import org.slf4j.LoggerFactory
import altitude.{Altitude, Cleaners, Const => C}
import net.codingwell.scalaguice.InjectorExtensions._


class MetadataService(app: Altitude) extends BaseService[Folder](app){
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[MetadataFieldDao]
}
