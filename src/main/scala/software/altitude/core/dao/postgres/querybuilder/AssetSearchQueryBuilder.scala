package software.altitude.core.dao.postgres.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.dao.jdbc.querybuilder.{ClauseComponents, SearchQueryBuilder, SqlQuery, SqlQueryBuilder}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}

class AssetSearchQueryBuilder(sqlColsForSelect: List[String])
  extends SearchQueryBuilder(sqlColsForSelect = sqlColsForSelect, tableNames = Set("asset")) {

  protected def textSearch(searchQuery: SearchQuery): ClauseComponents = {
    if (searchQuery.isText) {
      ClauseComponents(
        elements = List(s"$searchDocumentTable.tsv @@ to_tsquery(?)"),
        bindVals = List(searchQuery.text.get))
    }
    else {
      ClauseComponents()
    }
  }
}
