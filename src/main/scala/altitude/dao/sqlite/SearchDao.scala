package altitude.dao.sqlite

import altitude.transactions.TransactionId
import altitude.util.QueryResult
import altitude.{Const => C, Context, Altitude}
import altitude.models.Asset
import org.slf4j.LoggerFactory

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Sqlite {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def addSearchDocument(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
    val docSql =
      s"""
         INSERT INTO search_document (${C.Base.REPO_ID}, ${C.SearchToken.ASSET_ID}, body)
              VALUES (?, ?, ?)
       """

    val metadataValues = asset.metadata.data.foldLeft(Set[String]()) { (res, m) =>
      res ++ m._2
    }

    val body = asset.path + ' ' +
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
        SELECT *
        FROM search_document
        WHERE ${C.Base.REPO_ID} = ? AND body MATCH ?;
      """

    val countSql =
      s"""
        SELECT COUNT(*) AS count
        FROM search_document
        WHERE ${C.Base.REPO_ID} = ? AND body MATCH ?
      """

    val bindVals: List[Any] = List(ctx.repo.id.get, textQuery)
    val recs = manyBySqlQuery(sql, bindVals)
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