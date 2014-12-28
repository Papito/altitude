package dao.manager.mongo

import constants.{const => C}
import dao.mongo.BaseDao
import models.Asset
import util.log

class LibraryDao  extends BaseDao with dao.manager.LibraryDao {
  protected val COLLECTION_NAME = "assets"

  def addAsset(asset: Asset): Asset = {
    log.info("Adding asset", Map("asset" -> asset), C.tag.DB)
    asset
  }

}
