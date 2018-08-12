package software.altitude.core.dao.sqlite.querybuilder

import software.altitude.core.dao.jdbc.querybuilder.SqlQueryBuilder
import software.altitude.core.util.Query
import software.altitude.core.{Context, Const => C}

class AssetSearchQueryBuilder(sqlColsForSelect: String, tableName: String)
  extends SqlQueryBuilder(sqlColsForSelect = sqlColsForSelect, tableName = tableName) {

  override protected def getSqlBindVals(query: Query)(implicit ctx: Context): List[Any] = {
    super.getSqlBindVals(query)
  }

  override protected def getWhereClauses(query: Query)(implicit ctx: Context): List[String] = {
    s"asset.${C.Base.REPO_ID} = ?" ::
    s"search_document.${C.Base.REPO_ID} = ?" ::
    s"search_document.${C.SearchToken.ASSET_ID} = asset.id" ::
    s"body MATCH ?" ::
    "AND is_recycled = 0" :: Nil

  }
}
