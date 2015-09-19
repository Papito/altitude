package altitude.dao.postgres

import altitude.models.Preview
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._

class PreviewDao(app: Altitude) extends altitude.dao.jdbc.PreviewDao(app) with Postgres