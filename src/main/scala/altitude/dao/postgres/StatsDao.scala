package altitude.dao.postgres

import altitude.Altitude

class StatsDao(app: Altitude) extends altitude.dao.jdbc.StatsDao(app) with Postgres