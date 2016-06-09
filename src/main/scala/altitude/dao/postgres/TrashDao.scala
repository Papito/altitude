package altitude.dao.postgres

import altitude.Altitude

class TrashDao (app: Altitude) extends altitude.dao.jdbc.TrashDao(app) with Postgres