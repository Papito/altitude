package service.manager

//import constants.{const => C}

import dao.BaseDao
import net.codingwell.scalaguice.InjectorExtensions._

import dao.manager.LibraryDao
import models.Asset

class LibraryService extends BaseService[Asset, String] {
  override protected val DAO = app.injector.instance[LibraryDao]
}
