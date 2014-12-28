package dao.manager.mongo

import play.api.libs.json._
import _root_.util.log
import constants.{const => C}
import models.Asset
import play.api.libs.concurrent.Execution.Implicits._

class LibraryDao extends dao.mongo.BaseDao with dao.manager.LibraryDao {
  protected val COLLECTION_NAME = "assets"

  def addAsset(asset: Asset): Asset = {
    log.info("Adding asset", Map("asset" -> asset), C.tag.DB)
    collection.insert(Json.toJson(asset.toMap)).map(lastError =>
      log.error(lastError.toString))
    asset
  }

}
