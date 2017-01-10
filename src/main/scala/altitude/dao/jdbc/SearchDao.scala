package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const => C, Context, Altitude}
import altitude.models.{MetadataField, Asset}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

abstract class SearchDao(val app: Altitude) extends BaseJdbcDao("search_token") with altitude.dao.SearchDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()

  override def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])(implicit ctx: Context, txId: TransactionId) = {
    log.debug(s"Indexing asset $asset with metadata [${asset.metadata}]")

    asset.metadata.data.foreach { m =>
      val fieldId = m._1

      metadataFields.contains(fieldId) match {
        case false =>
          log.error(s"Asset $asset contains metadata field ID [$fieldId] that is not part of field configuration!")

        case true =>
          val metadataField = metadataFields(fieldId)
          val values = m._2
          log.debug(s"Processing field [${metadataField.nameLowercase}] with values [$values]")

          val sql = s"""
            INSERT INTO $tableName (
                 ${C.SearchToken.REPO_ID}, ${C.SearchToken.ASSET_ID},
                 ${C.SearchToken.FIELD_ID}, ${C.SearchToken.FIELD_VALUE_TXT})
                VALUES(?, ?, ?, ?)
            """

          values.foreach { value =>
            val sqlVals: List[Object] = List(
              ctx.repo.id.get,
              asset.id.get,
              metadataField.id.get,
              value)

            log.debug(s"INSERT SQL: $sql. ARGS: ${values.toString()}")

            val runner: QueryRunner = new QueryRunner()
            runner.update(conn, sql, sqlVals:_*)
          }
      }
    }
  }
}

