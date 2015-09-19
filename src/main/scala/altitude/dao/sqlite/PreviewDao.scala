package altitude.dao.sqlite

import altitude.{Altitude, Const => C}

class PreviewDao(app: Altitude) extends altitude.dao.jdbc.PreviewDao(app) with Sqlite