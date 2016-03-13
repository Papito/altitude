package altitude.dao.postgres

import altitude.{Altitude, Const => C}

class ImportProfileDao(app: Altitude) extends  altitude.dao.jdbc.ImportProfileDao(app) with Postgres

