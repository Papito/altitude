package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude}
import altitude.models.{BaseModel, Folder}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, JsObject}

abstract class FolderDao(val app: Altitude) extends BaseJdbcDao("folder") with altitude.dao.FolderDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Folder(
      id = Some(rec.get(C.Folder.ID).get.asInstanceOf[String]),
      name = rec.get(C.Folder.NAME).get.asInstanceOf[String],
      parentId = Some(rec.get(C.Folder.PARENT_ID).get.asInstanceOf[String]),
      size = rec.get(C.Folder.SIZE).get.asInstanceOf[Int]
    )
    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val folder = jsonIn: Folder

    val folderId = BaseModel.genId
    val parentId = folder.parentId match {
      case None => folderId
      case _ => folder.parentId.get
    }

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Folder.NAME}, ${C.Folder.PARENT_ID})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?)
    """

    val sqlVals: List[Object] = List(
      folder.name,
      parentId)

    addRecord(jsonIn, sql, sqlVals, id = Some(folderId)) ++
      JsObject(Seq(C.Folder.PARENT_ID -> JsString(parentId)))
  }
}