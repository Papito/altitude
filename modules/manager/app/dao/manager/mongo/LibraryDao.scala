package dao.manager.mongo

import dao.mongo.BaseDao
import play.api.libs.json._
import _root_.util.log
import constants.{const => C}
import models.Asset
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.LastError
import reactivemongo.extensions.json.dao.JsonDao
import play.modules.reactivemongo.json.BSONFormats._

import scala.concurrent.Future

class LibraryDao extends JsonDao[Asset, BSONObjectID](BaseDao.db, "asset") with dao.manager.LibraryDao {
  def addAsset(asset: Asset): Future[LastError] = {
    log.info("Adding asset", Map("asset" -> asset), C.tag.DB)
    collection.insert(Json.toJson(asset.toMap))
  }

}
