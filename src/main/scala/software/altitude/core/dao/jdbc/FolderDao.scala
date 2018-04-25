package software.altitude.core.dao.jdbc

import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.models.Folder
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{AltitudeCoreApp, Const => C, Context}

abstract class FolderDao(val app: AltitudeCoreApp) extends BaseJdbcDao with software.altitude.core.dao.FolderDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override final val tableName = "folder"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Folder(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      name = rec(C.Folder.NAME).asInstanceOf[String],
      parentId = rec(C.Folder.PARENT_ID).asInstanceOf[String],
      numOfAssets = rec(C.Folder.NUM_OF_ASSETS).asInstanceOf[Int]
    )
    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val folder = jsonIn: Folder

    val sql = s"""
        INSERT INTO $tableName (
             $coreSqlColsForInsert,
             ${C.Folder.NAME}, ${C.Folder.NAME_LC}, ${C.Folder.PARENT_ID})
            VALUES ($coreSqlValsForInsert, ?, ?, ?)
    """

    val sqlVals: List[Any] = List(
      folder.name,
      folder.nameLowercase,
      folder.parentId)

    addRecord(jsonIn, sql, sqlVals)
  }
}