package altitude.dao.sqlite

import altitude.Altitude

class FolderDao(app: Altitude) extends altitude.dao.jdbc.FolderDao(app) with Sqlite
