package software.altitude.core.dao.sqlite.querybuilder

import org.slf4j.LoggerFactory
import software.altitude.core.dao.jdbc.querybuilder.{SearchQueryBuilder, SqlQuery, SqlQueryBuilder}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.Query.QueryParam
import software.altitude.core.util.{Query, SearchQuery}
import software.altitude.core.{Context, Const => C}

class AssetSearchQueryBuilder(sqlColsForSelect: List[String], tableNames: Set[String])
    extends SearchQueryBuilder(sqlColsForSelect = sqlColsForSelect, tableNames = tableNames) {

  private final val log = LoggerFactory.getLogger(getClass)
  private val searchParamTable = "search_parameter"
  private val searchDocumentTable = "search_document"

}