package service.manager

import constants.{const => C}
import models.Asset
import util.log

class LibraryService extends BaseService {
  val DAO: dao.manager.LibraryDao = new dao.manager.mongo.LibraryDao

  def addAsset(asset: Asset): Asset = {
    log.info("Adding asset", Map("asset" -> asset), C.tag.SERVICE)
    DAO.addAsset(asset)
  }
}
