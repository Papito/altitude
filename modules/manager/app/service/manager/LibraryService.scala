package service.manager

import constants.{const => C}
import models.Asset
import reactivemongo.core.commands.LastError
import util.log

import scala.concurrent.Future

class LibraryService extends BaseService[Asset] {
  override val DAO = new dao.manager.mongo.LibraryDao
}
