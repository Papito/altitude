package altitude.dao.sqlite

import altitude.Altitude

class RepositoryDao(app: Altitude) extends altitude.dao.jdbc.RepositoryDao(app) with Sqlite
