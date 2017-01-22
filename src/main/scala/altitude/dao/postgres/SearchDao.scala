package altitude.dao.postgres

import java.sql.{Types, PreparedStatement}

import altitude.transactions.TransactionId
import altitude.util.QueryResult
import altitude.{Const => C, Context, Altitude}
import altitude.models.{FieldType, MetadataField, Asset}
import org.slf4j.LoggerFactory

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Postgres {
  private final val log = LoggerFactory.getLogger(getClass)

  override def indexAsset(asset: Asset, metadataFields: Map[String, MetadataField])
                         (implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Indexing asset $asset with metadata [${asset.metadata}]")

    /**
     * Create the search document
     */
    val docSql =
      s"""
         INSERT INTO search_document (
                     ${C.SearchToken.REPO_ID}, ${C.SearchToken.ASSET_ID}, ${C.Asset.PATH},
                     metadata_values, body)
              VALUES (?, ?, ?, ?, ?)
       """

    val metadataValues = asset.metadata.data.foldLeft(Set[String]()) { (res, m) =>
      res ++ m._2
    }

    val sqlVals: List[Any] = List(
      ctx.repo.id.get,
      asset.id.get,
      asset.path,
      metadataValues.mkString(" "),
      "")

    addRecord(asset, docSql, sqlVals)

    /**
     * Index each metadata value as a strongly-typed dimension
     */
    asset.metadata.data.foreach { m =>
      val fieldId = m._1

      metadataFields.contains(fieldId) match {
        case false =>
          log.error(s"Asset $asset contains metadata field ID [$fieldId] that is not part of field configuration!")

        case true =>
          val field = metadataFields(fieldId)
          val values = m._2
          log.debug(s"Processing field [${field.nameLowercase}] with values [$values]")

          val sql =  s"""
            INSERT INTO $tableName (
                        ${C.SearchToken.REPO_ID}, ${C.SearchToken.ASSET_ID},
                        ${C.SearchToken.FIELD_ID},
                        ${C.SearchToken.FIELD_VALUE_TXT},
                        ${C.SearchToken.FIELD_VALUE_KW},
                        ${C.SearchToken.FIELD_VALUE_NUM},
                        ${C.SearchToken.FIELD_VALUE_BOOL})
                 VALUES (?, ?, ?, ?, ?, ?, ?)
            """

          log.debug(s"INSERT SQL: $sql. ARGS: ${values.toString()}")

          val preparedStatement: PreparedStatement = conn.prepareStatement(sql)

          values.foreach { value =>
            //log.debug(s"Executing for [${field.name}] and [$value]")

            preparedStatement.clearParameters()
            preparedStatement.setString(1, ctx.repo.id.get)
            preparedStatement.setString(2, asset.id.get)
            preparedStatement.setString(3, field.id.get)
            preparedStatement.setString(4, value)
            // keyword
            if (field.fieldType == FieldType.KEYWORD) {
              preparedStatement.setString(5, value.toLowerCase)
            } else {
              preparedStatement.setNull(5, Types.VARCHAR)
            }
            // number
            if (field.fieldType == FieldType.NUMBER) {
              preparedStatement.setDouble(6, value.toDouble)
            } else {
              preparedStatement.setNull(6, Types.DOUBLE)
            }
            // boolean
            if (field.fieldType == FieldType.BOOL) {
              preparedStatement.setBoolean(7, value.toBoolean)
            } else {
              preparedStatement.setNull(7, Types.BOOLEAN)
            }

            preparedStatement.execute()
          }
      }
    }
  }

  override def search(textQuery: String)
                     (implicit ctx: Context, txId: TransactionId): QueryResult = {
    val sql =
      s"""
        SELECT *
        FROM search_document
        WHERE tsv @@ to_tsquery(?)
      """

    val countSql =
      s"""
        SELECT COUNT(*) AS count
        FROM search_document
        WHERE tsv @@ to_tsquery(?)
      """

    val recs = manyBySqlQuery(sql, List(textQuery))
    val count: Int = getQueryResultCountBySql(countSql, List(textQuery))

    log.debug(s"Found [$count] records. Retrieved [${recs.length}] records")
    if (recs.nonEmpty) {
      log.debug(recs.map(_.toString()).mkString("\n"))
    }

    log.debug(s"Found [$count] records. Retrieved [${recs.length}] records")
    if (recs.nonEmpty) {
      log.debug(recs.map(_.toString()).mkString("\n"))
    }
    QueryResult(records = recs.map{makeModel}, total = count, query = None)
  }
}
