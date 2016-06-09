package altitude.dao.sqlite

import altitude.Altitude

class TrashDao(app: Altitude) extends altitude.dao.jdbc.TrashDao(app) with Sqlite