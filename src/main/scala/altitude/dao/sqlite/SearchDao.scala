package altitude.dao.sqlite

import altitude.models.Asset
import altitude.transactions.TransactionId
import altitude.util.QueryResult
import altitude.{Altitude, Const => C, Context}
import org.slf4j.LoggerFactory

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Sqlite {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def addSearchDocument(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
    require(asset.path.isEmpty)

    val path = app.service.fileStore.getAssetPath(asset)

    val docSql =
      s"""
         INSERT INTO search_document (${C.Base.REPO_ID}, ${C.SearchToken.ASSET_ID}, body)
              VALUES (?, ?, ?)
       """

    val metadataValues = asset.metadata.data.foldLeft(Set[String]()) { (res, m) =>
      res ++ m._2
    }

    val body = path + ' ' +
      metadataValues.mkString(" ") + ' ' +
      "" // body

    val sqlVals: List[Any] = List(
      ctx.repo.id.get, asset.id.get, body)

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
           AND body MATCH ?
      """

    val selectSql = sql.format(AssetDao.DEFAULT_SQL_COLS_FOR_SELECT)

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