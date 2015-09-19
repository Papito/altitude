package altitude.dao.sqlite

import altitude.{Altitude, Const => C}


class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Sqlite