package altitude.dao.mongo

import altitude.Altitude
import altitude.models.Folder
import altitude.transactions.TransactionId
import com.mongodb.casbah.Imports._
import altitude.{Const => C}
import play.api.libs.json.{Json, JsObject}

import scala.collection.mutable


class FolderDao(val app: Altitude) extends BaseMongoDao("folders") with altitude.dao.FolderDao