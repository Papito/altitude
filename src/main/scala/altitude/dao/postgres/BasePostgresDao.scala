package altitude.dao.postgres

import altitude.dao.jdbc.BaseJdbcDao
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

abstract class BasePostgresDao(val tableName: String) extends BaseJdbcDao {
  override val log = LoggerFactory.getLogger(getClass)
}
