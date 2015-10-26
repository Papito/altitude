package altitude.dao.postgres

import altitude.Altitude

class FolderDao(app: Altitude) extends altitude.dao.jdbc.FolderDao(app) with Postgres
