package service.manager

//import constants.{const => C}

import dao.manager.LibraryDao
import models.Asset
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.Play

class LibraryService extends BaseService[Asset, String] {
  override protected val DAO = app.injector.instance[LibraryDao]
}
