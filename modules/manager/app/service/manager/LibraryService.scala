package service.manager

//import constants.{const => C}
import models.Asset

class LibraryService extends BaseService[Asset] {
  override val DAO = new dao.manager.mongo.LibraryDao
}
