package altitude.dao.postgres

import altitude.{Const => C, Altitude}
import altitude.models.{ImportProfile, MediaType}
import altitude.transactions.TransactionId
import play.api.libs.json.{Json, JsObject}

class ImportProfileDao(app: Altitude) extends  altitude.dao.jdbc.ImportProfileDao(app) with Postgres

