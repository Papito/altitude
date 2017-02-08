package altitude.dao.jdbc

import altitude.models.Folder
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import play.api.libs.json.JsObject

abstract class FolderDao(val app: Altitude) extends BaseJdbcDao("folder") with altitude.dao.FolderDao {

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Folder(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      name = rec.get(C.Folder.NAME).get.asInstanceOf[String],
      parentId = rec.get(C.Folder.PARENT_ID).get.asInstanceOf[String],
      numOfAssets = rec.get(C.Folder.NUM_OF_ASSETS).get.asInstanceOf[Int],
      path = rec.get(C.Folder.PATH).get.asInstanceOf[String]
    )
    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val folder = jsonIn: Folder

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT,
             ${C.Folder.NAME}, ${C.Folder.NAME_LC}, ${C.Folder.PARENT_ID}, ${C.Folder.PATH})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?)
    """

    val sqlVals: List[Any] = List(
      folder.name,
      folder.nameLowercase,
      folder.parentId,
      folder.path)

    addRecord(jsonIn, sql, sqlVals)
  }
}