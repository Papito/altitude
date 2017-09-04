package software.altitude.core.dao.jdbc

import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.models.User
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Const => C, Context, AltitudeCoreApp}

abstract class UserDao(val app: AltitudeCoreApp) extends BaseJdbcDao with software.altitude.core.dao.UserDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override final val TABLE_NAME = "repository_user"

  override protected val ONE_SQL = s"""
      SELECT $DEFAULT_SQL_COLS_FOR_SELECT
        FROM $TABLE_NAME
       WHERE ${C.Base.ID} = ?"""


  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = User(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String])
    )

    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val user = jsonIn: User

    val sql = s"""
        INSERT INTO $TABLE_NAME (${C.Base.ID})
             VALUES (?)
    """

    addRecord(jsonIn, sql, List())
  }

  override def getById(id: String)(implicit ctx: Context, txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id' from '$TABLE_NAME'", C.LogTag.DB)
    val rec: Option[Map[String, AnyRef]] = oneBySqlQuery(ONE_SQL, List(id))
    if (rec.isDefined) Some(makeModel(rec.get)) else None
  }

  override protected def combineInsertValues(id: String, vals: List[Any])(implicit  ctx: Context) =
    id :: vals
}
