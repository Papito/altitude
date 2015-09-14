package altitude.dao.sqlite

import altitude.dao.jdbc.BaseJdbcDao
import org.slf4j.LoggerFactory

abstract class BaseSqliteDao (val tableName: String) extends BaseJdbcDao {
  override val log = LoggerFactory.getLogger(getClass)
}
