package altitude.dao.jdbc

import altitude.models.Repository
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class RepositoryDao(val app: Altitude) extends BaseJdbcDao("repository") with altitude.dao.RepositoryDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected val ONE_SQL = s"""
      SELECT $DEFAULT_SQL_COLS_FOR_SELECT
        FROM $tableName
       WHERE ${C.Base.ID} = ?"""

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Repository(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      name = rec.get(C.Repository.NAME).get.asInstanceOf[String],
      rootFolderId = rec.get(C.Repository.ROOT_FOLDER_ID).get.asInstanceOf[String],
      uncatFolderId = rec.get(C.Repository.UNCAT_FOLDER_ID).get.asInstanceOf[String]
    )

    model
  }

  override def getById(id: String)(implicit ctx: Context, txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id' from '$tableName'", C.LogTag.DB)
    val rec: Option[Map[String, AnyRef]] = oneBySqlQuery(ONE_SQL, List(id))
    if (rec.isDefined) Some(makeModel(rec.get)) else None
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val repo = jsonIn: Repository

    val sql = s"""
        INSERT INTO $tableName (
             ${C.Base.ID}, ${C.Repository.NAME},
             ${C.Repository.ROOT_FOLDER_ID}, ${C.Repository.UNCAT_FOLDER_ID})
            VALUES (?, ?, ?, ?)
    """

    val sqlVals: List[Object] = List(
      repo.name,
      repo.rootFolderId,
      repo.uncatFolderId)

    addRecord(jsonIn, sql, sqlVals)
  }

  override protected def combineInsertValues(id: String, vals: List[Object])(implicit  ctx: Context) = id :: vals
}
