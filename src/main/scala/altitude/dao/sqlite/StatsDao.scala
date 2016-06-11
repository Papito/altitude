package altitude.dao.sqlite

import altitude.Altitude

class StatsDao(app: Altitude) extends altitude.dao.jdbc.StatsDao(app) with Sqlite
