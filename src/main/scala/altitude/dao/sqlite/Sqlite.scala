package altitude.dao.sqlite

import altitude.dao.jdbc.BaseJdbcDao
import org.slf4j.{Logger, LoggerFactory}

trait Sqlite {
  val log: Logger
  val tableName: String

}