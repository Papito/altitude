package altitude.services

//import constants.{const => C}

import altitude.dao.LibraryDao
import altitude.models.Asset
import net.codingwell.scalaguice.InjectorExtensions._

class LibraryService extends BaseService[Asset] {
  override protected val DAO = app.injector.instance[LibraryDao]
}
