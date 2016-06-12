package altitude.dao.sqlite

import altitude.Altitude

class StatDao(app: Altitude) extends altitude.dao.jdbc.StatDao(app) with Sqlite {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
