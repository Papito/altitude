package altitude.dao.postgres

import altitude.Altitude

class StatDao(app: Altitude) extends altitude.dao.jdbc.StatDao(app) with Postgres {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}