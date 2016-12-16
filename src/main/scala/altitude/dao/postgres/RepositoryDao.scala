package altitude.dao.postgres

import altitude.Altitude

class RepositoryDao(app: Altitude) extends altitude.dao.jdbc.RepositoryDao(app) with Postgres