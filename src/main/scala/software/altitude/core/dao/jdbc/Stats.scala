package software.altitude.core.dao.jdbc

trait Stats { this: BaseJdbcDao =>
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
