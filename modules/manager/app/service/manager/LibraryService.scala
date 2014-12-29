package service.manager

import constants.{const => C}
import models.Asset
import reactivemongo.core.commands.LastError
import util.log

import scala.concurrent.Future

class LibraryService extends BaseService {
  val DAO: dao.manager.LibraryDao = new dao.manager.mongo.LibraryDao

  def addAsset(asset: Asset): Future[LastError]  = {
    //log.info("Adding asset", Map("asset" -> asset), C.tag.SERVICE)
    DAO.addAsset(asset)
  }
}
