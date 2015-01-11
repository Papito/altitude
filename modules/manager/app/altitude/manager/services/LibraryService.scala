package altitude.manager.services

//import constants.{const => C}

import altitude.common.models.Asset
import altitude.manager.dao.LibraryDao
import net.codingwell.scalaguice.InjectorExtensions._

class LibraryService extends BaseService[Asset, String] {
  override protected val DAO = app.injector.instance[LibraryDao]
}
