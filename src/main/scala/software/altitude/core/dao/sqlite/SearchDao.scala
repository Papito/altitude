package software.altitude.core.dao.sqlite

import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.RequestContext
import software.altitude.core.dao.sqlite.querybuilder.AssetSearchQueryBuilder
import software.altitude.core.models.Asset
import software.altitude.core.util.SearchQuery
import software.altitude.core.util.SearchResult
import software.altitude.core.{Const => C}

class SearchDao(override val appContext: Altitude) extends software.altitude.core.dao.jdbc.SearchDao(appContext) with SqliteOverrides {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def addSearchDocument(asset: Asset): Unit = {
    val docSql =
      s"""
         INSERT INTO search_document (${C.Base.REPO_ID}, ${C.SearchToken.ASSET_ID}, body)
              VALUES (?, ?, ?)
       """

    val metadataValues = asset.metadata.data.foldLeft(Set[String]()) { (res, m) =>
      res ++ m._2.map(_.value)
    }

    val body = metadataValues.mkString(" ")

    val sqlVals: List[Any] = List(
      RequestContext.getRepository.id.get, asset.id.get, body)

    addRecord(asset, docSql, sqlVals)
  }

  override protected def replaceSearchDocument(asset: Asset): Unit = {
    val docSql =
      s"""
         UPDATE search_document
            SET body = ?
          WHERE ${C.Base.REPO_ID} = ?
            AND ${C.SearchToken.ASSET_ID} = ?
       """

    val metadataValues = asset.metadata.data.foldLeft(Set[String]()) { (res, m) =>
      res ++ m._2.map(_.value)
    }

    val body = metadataValues.mkString(" ")

    val sqlVals: List[Any] = List(
      body, RequestContext.getRepository.id.get, asset.id.get)

    addRecord(asset, docSql, sqlVals)

    val runner: QueryRunner = new QueryRunner()
    runner.update(RequestContext.getConn, docSql, sqlVals.map(_.asInstanceOf[Object]): _*)
  }

  override def search(searchQuery: SearchQuery): SearchResult = {
    val sqlQueryBuilder = new AssetSearchQueryBuilder(sqlColsForSelect = columnsForSelect)

    val sqlQuery = sqlQueryBuilder.buildSelectSql(query = searchQuery)
    val recs = manyBySqlQuery(sqlQuery.sqlAsString, sqlQuery.bindValues)
    val total: Int = count(recs)

    log.debug(s"Found [$total] records. Retrieved [${recs.length}] records")

    if (recs.nonEmpty) {
      log.debug(recs.map(_.toString()).mkString("\n"))
    }

    log.debug(s"Found [$total] records. Retrieved [${recs.length}] records")
    if (recs.nonEmpty) {
      log.debug(recs.map(_.toString()).mkString("\n"))
    }

    SearchResult(
      records = recs.map{makeModel},
      total = total,
      rpp = searchQuery.rpp,
      sort = searchQuery.searchSort.toList)
  }
}
