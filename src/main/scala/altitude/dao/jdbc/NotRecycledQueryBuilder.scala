package altitude.dao.jdbc

import altitude.{Const => C}

class NotRecycledQueryBuilder(sqlColsForSelect: String, tableName: String)
  extends SqlQueryBuilder(sqlColsForSelect, tableName) {

  override protected def assembleQuery(select: String, from: String, where: String, rpp: Int = 0, page: Int = 0): String = {
    super.assembleQuery(select, from, s"$where AND ${C.Asset.IS_RECYCLED} = 0", rpp, page)
  }
}