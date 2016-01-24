package altitude.dao.mongo

import altitude.Altitude
import altitude.models.Folder
import altitude.transactions.TransactionId
import com.mongodb.casbah.Imports._
import altitude.{Const => C}
import play.api.libs.json.{Json, JsObject}

import scala.collection.mutable


class FolderDao(val app: Altitude) extends BaseMongoDao("folders") with altitude.dao.FolderDao {
  override def getSystemFolders(implicit txId: TransactionId): Map[String, Folder] = {
    val options = MongoDBObject(C.System.TRASH_COUNT -> 1, C.System.UNCATEGORIZED_COUNT -> 1)
    val cursor: MongoCursor = BaseMongoDao.DB.get("system").find(MongoDBObject(), options)
    val record: DBObject = cursor.one()

    if (record == null) {
      return Map(
        Folder.UNCATEGORIZED.id.get -> Folder.TRASH,
        Folder.TRASH.id.get -> Folder.TRASH)
    }

    val json: JsObject = Json.parse(record.toString).as[JsObject]

    val folders = mutable.Map[String, Folder]()

    json.fieldSet.foreach{v =>
      if (v._1 == C.System.UNCATEGORIZED_COUNT) {
        folders += Folder.UNCATEGORIZED.id.get -> new Folder(
          id = Folder.UNCATEGORIZED.id,
          name = Folder.UNCATEGORIZED.name,
          numOfAssets = v._2.asInstanceOf[Int])
      }

      if (v._1 == C.System.TRASH_COUNT) {
        folders += Folder.TRASH.id.get -> new Folder(
          id = Folder.TRASH.id,
          name = Folder.TRASH.name,
          numOfAssets = v._2.asInstanceOf[Int])
      }
    }

    folders.toMap
  }
}