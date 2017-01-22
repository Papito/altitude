package altitude.dao.sqlite

import altitude.Altitude

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Sqlite