package altitude.dao.postgres

import java.sql.{Types, PreparedStatement}

import altitude.transactions.TransactionId
import altitude.util.QueryResult
import altitude.{Const => C, Context, Altitude}
import altitude.models.{FieldType, MetadataField, Asset}
import org.slf4j.LoggerFactory

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Postgres {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def addSearchDocument(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
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
  }

  override def search(textQuery: String)
                     (implicit ctx: Context, txId: TransactionId): QueryResult = {
    val sql =
      s"""
        SELECT %s
          FROM search_document, asset
         WHERE asset.${C.Base.REPO_ID} = ?
           AND search_document.${C.Base.REPO_ID} = ?
           AND search_document.${C.SearchToken.ASSET_ID} = asset.id
           AND search_document.tsv @@ to_tsquery(?)
      """

    val selectSql = sql.format(
      s"""
        asset.*,
        (${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA},
        (${C.Asset.EXTRACTED_METADATA}#>>'{}')::text as ${C.Asset.EXTRACTED_METADATA},
        EXTRACT(EPOCH FROM asset.created_at) AS created_at,
        EXTRACT(EPOCH FROM asset.updated_at) AS updated_at
      """)

    val countSql = sql.format("COUNT(*) as count")

    val bindVals: List[Any] = List(ctx.repo.id.get, ctx.repo.id.get, textQuery)

    val recs = manyBySqlQuery(selectSql, bindVals)
    val count: Int = getQueryResultCountBySql(countSql, bindVals)

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
