package altitude.dao.jdbc

import java.sql.PreparedStatement

import altitude.transactions.TransactionId
import altitude.{Const => C, Context, Altitude}
import altitude.models.{MetadataField, Asset}
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

          log.debug(s"INSERT SQL: $sql. ARGS: ${values.toString()}")

          val preparedStatement: PreparedStatement = conn.prepareStatement(sql)

          values.foreach { value =>
            log.debug(s"Executing for [${metadataField.name}] and [$value]")
            preparedStatement.clearParameters()
            preparedStatement.setString(1, ctx.repo.id.get)
            preparedStatement.setString(2, asset.id.get)
            preparedStatement.setString(3, metadataField.id.get)
            preparedStatement.setString(4, value)
            preparedStatement.execute()
          }
      }
    }
  }
}

