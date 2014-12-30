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

class LibraryDao extends BaseDao[Asset]("assets") with dao.manager.LibraryDao
