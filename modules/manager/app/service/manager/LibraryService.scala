package service.manager

//import constants.{const => C}
import net.codingwell.scalaguice.InjectorExtensions._

import dao.manager.mongo.LibraryDao
import models.Asset

class LibraryService extends BaseService[Asset, String] {
  val LibraryDao: LibraryDao = app.injector.instance[LibraryDao]

  override def DAO = LibraryDao
}
