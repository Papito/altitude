package altitude.dao.postgres

import altitude.models.{Asset, MediaType}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import play.api.libs.json._


class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Postgres