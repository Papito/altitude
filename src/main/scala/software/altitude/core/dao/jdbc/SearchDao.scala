package software.altitude.core.dao.jdbc

import java.sql.{PreparedStatement, Types}

import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.models._
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.QueryResult
import software.altitude.core.{Altitude, Context, Const => C}

abstract class SearchDao(override val app: Altitude) extends AssetDao(app) with software.altitude.core.dao.SearchDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override def search(textQuery: String)(implicit ctx: Context, txId: TransactionId): QueryResult =
    throw new NotImplementedError

  protected def addSearchDocument(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit =
    throw new NotImplementedError

  override def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])
                         (implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Indexing asset $asset with metadata [${asset.metadata}]")

    indexMetadata(asset, metadataFields)

    addSearchDocument(asset)
  }

  override protected def addRecord(jsonIn: JsObject, q: String, values: List[Any])
                                  (implicit ctx: Context, txId: TransactionId): JsObject = {
    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, q, values.map(_.asInstanceOf[Object]): _*)
    jsonIn
  }

  protected def indexMetadata(asset: Asset, metadataFields: Map[String, MetadataField])
                             (implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Indexing metadata for asset $asset: [${asset.metadata}]")

    asset.metadata.data.foreach { m =>
      val fieldId = m._1

      if (!metadataFields.contains(fieldId)) {
        log.error(s"Asset $asset contains metadata field ID [$fieldId] that is not part of field configuration!")
        return
      }

      val field = metadataFields(fieldId)
      val values = m._2
      log.debug(s"Processing field [${field.nameLowercase}] with values [$values]")

      val sql = s"""
            INSERT INTO search_parameter (
                        ${C.SearchToken.REPO_ID}, ${C.SearchToken.ASSET_ID},
                        ${C.SearchToken.FIELD_ID},
                        ${C.SearchToken.FIELD_VALUE_KW},
                        ${C.SearchToken.FIELD_VALUE_NUM},
                        ${C.SearchToken.FIELD_VALUE_BOOL})
                 VALUES (?, ?, ?, ?, ?, ?)
            """

      log.debug(s"INSERT SQL: $sql. ARGS: ${values.toString()}")

      val preparedStatement: PreparedStatement = conn.prepareStatement(sql)

      values.foreach { mdVal =>
        preparedStatement.clearParameters()
        preparedStatement.setString(1, ctx.repo.id.get)
        preparedStatement.setString(2, asset.id.get)
        preparedStatement.setString(3, field.id.get)

        // keyword
        if (field.fieldType == FieldType.KEYWORD) {
          preparedStatement.setString(4, mdVal.value.toLowerCase)
        } else {
          preparedStatement.setNull(4, Types.VARCHAR)
        }
        // number
        if (field.fieldType == FieldType.NUMBER) {
          preparedStatement.setDouble(5, mdVal.value.toDouble)
        } else {
          preparedStatement.setNull(5, Types.DOUBLE)
        }
        // boolean
        if (field.fieldType == FieldType.BOOL) {
          preparedStatement.setBoolean(6, mdVal.value.toBoolean)
        } else {
          preparedStatement.setNull(6, Types.BOOLEAN)
        }

        preparedStatement.execute()
      }
    }
  }
}

