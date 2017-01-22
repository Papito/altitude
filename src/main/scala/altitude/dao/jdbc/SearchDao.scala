package altitude.dao.jdbc

import java.sql.{Types, PreparedStatement}

import altitude.transactions.TransactionId
import altitude.util.{QueryResult, Query}
import altitude.{Const => C, Context, Altitude}
import altitude.models.{FieldType, MetadataField, Asset}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

abstract class SearchDao(val app: Altitude) extends BaseJdbcDao("search_parameter") with altitude.dao.SearchDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()

  override def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])
                         (implicit ctx: Context, txId: TransactionId): Unit = throw new NotImplementedError

  override def search(textQuery: String)(implicit ctx: Context, txId: TransactionId): QueryResult =
    throw new NotImplementedError

  override protected def addRecord(jsonIn: JsObject, q: String, values: List[Any])
                                  (implicit ctx: Context, txId: TransactionId): JsObject = {
    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, q, values.map(_.asInstanceOf[Object]):_*)
    jsonIn
  }
}

