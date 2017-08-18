package software.altitude.core.dao.jdbc

import software.altitude.core.models.Folder
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Altitude, Const => C, Context}
import play.api.libs.json.JsObject
import software.altitude.core.models.Folder
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Const, Context}
import software.altitude.core.models.Folder

abstract class FolderDao(val app: Altitude) extends BaseJdbcDao("folder") with software.altitude.core.dao.FolderDao {

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
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT,
             ${Const.Folder.NAME}, ${Const.Folder.NAME_LC}, ${Const.Folder.PARENT_ID})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?)
    """

    val sqlVals: List[Any] = List(
      folder.name,
      folder.nameLowercase,
      folder.parentId)

    addRecord(jsonIn, sql, sqlVals)
  }
}