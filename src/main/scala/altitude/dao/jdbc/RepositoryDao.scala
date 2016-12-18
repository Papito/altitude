package altitude.dao.jdbc

import altitude.models.{Repository, User}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class RepositoryDao(val app: Altitude) extends BaseJdbcDao("repository") with altitude.dao.RepositoryDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Repository(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      name = rec.get(C.Repository.NAME).get.asInstanceOf[String],
      rootFolderId = Some(rec.get(C.Repository.ROOT_FOLDER_ID).get.asInstanceOf[String]),
      uncatFolderId = Some(rec.get(C.Repository.UNCAT_FOLDER_ID).get.asInstanceOf[String])
    )

    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit user: User, txId: TransactionId): JsObject = {
    val repo = jsonIn: Repository

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Repository.NAME},
             ${C.Repository.ROOT_FOLDER_ID}, ${C.Repository.UNCAT_FOLDER_ID})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?)
    """

    val sqlVals: List[Object] = List(
      repo.name,
      repo.rootFolderId.get,
      repo.uncatFolderId.get)

    addRecord(jsonIn, sql, sqlVals)
  }
}
