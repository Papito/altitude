package altitude.dao.postgres

import altitude.Altitude


class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Postgres