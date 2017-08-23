package software.altitude.core.dao.jdbc

import play.api.libs.json.JsObject
import software.altitude.core.models.Folder
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Const => C, AltitudeCoreApp, Altitude, Context}

abstract class FolderDao(val app: AltitudeCoreApp) extends BaseJdbcDao("folder") with software.altitude.core.dao.FolderDao {

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Folder(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      name = rec.get(C.Folder.NAME).get.asInstanceOf[String],
      parentId = rec.get(C.Folder.PARENT_ID).get.asInstanceOf[String],
      numOfAssets = rec.get(C.Folder.NUM_OF_ASSETS).get.asInstanceOf[Int]
    )
    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val folder = jsonIn: Folder

    val sql = s"""
        INSERT INTO $TABLE_NAME (
             $CORE_SQL_COLS_FOR_INSERT,
             ${C.Folder.NAME}, ${C.Folder.NAME_LC}, ${C.Folder.PARENT_ID})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?)
    """

    val sqlVals: List[Any] = List(
      folder.name,
      folder.nameLowercase,
      folder.parentId)

    addRecord(jsonIn, sql, sqlVals)
  }
}