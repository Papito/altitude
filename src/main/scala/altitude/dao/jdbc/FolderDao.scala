package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude}
import altitude.models.{BaseModel, Folder}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, JsString, JsObject}
import scala.collection.mutable

abstract class FolderDao(val app: Altitude) extends BaseJdbcDao("folder") with altitude.dao.FolderDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Folder(
      id = Some(rec.get(C.Folder.ID).get.asInstanceOf[String]),
      name = rec.get(C.Folder.NAME).get.asInstanceOf[String],
      parentId = rec.get(C.Folder.PARENT_ID).get.asInstanceOf[String],
      numOfAssets = rec.get(C.Folder.NUM_OF_ASSETS).get.asInstanceOf[Int]
    )
    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val folder = jsonIn: Folder

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Folder.NAME}, ${C.Folder.NAME_LC}, ${C.Folder.PARENT_ID})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?)
    """

    val sqlVals: List[Object] = List(
      folder.name,
      folder.nameLowercase,
      folder.parentId)

    addRecord(jsonIn, sql, sqlVals)
  }

  override def getSystemFolders(implicit txId: TransactionId): Map[String, Folder] = {
    val sql = s"""
      SELECT ${C.System.TRASH_COUNT}, ${C.System.UNCATEGORIZED_COUNT}
        FROM system
        WHERE id = 0
      """

    log.debug(sql)
    val recs = manyBySqlQuery(sql)

    val folders = mutable.Map[String, Folder]()

    recs.foreach {rec =>
      if (rec.get(C.System.UNCATEGORIZED_COUNT).isDefined) {
        val count = rec.get(C.System.UNCATEGORIZED_COUNT).get.asInstanceOf[Int]

        folders += Folder.UNCATEGORIZED.id.get -> new Folder(
          id = Folder.UNCATEGORIZED.id,
          name = Folder.UNCATEGORIZED.name,
          numOfAssets = count)
      }

      if (rec.get(C.System.TRASH_COUNT).isDefined) {
        val count = rec.get(C.System.TRASH_COUNT).get.asInstanceOf[Int]

        folders += Folder.TRASH.id.get -> new Folder(
          id = Folder.TRASH.id,
          name = Folder.TRASH.name,
          numOfAssets = count)
      }
    }

    folders.toMap
  }
}