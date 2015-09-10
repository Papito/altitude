package altitude.service

import altitude.Validators.Validator
import altitude.Altitude
import altitude.dao.ImportProfileDao
import altitude.models.{ImportProfile}
import altitude.transactions.TransactionId
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import altitude.{Const => C}

class ImportProfileService(app: Altitude) extends BaseService[ImportProfile](app) {
  val log =  LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[ImportProfileDao]

  val IMPORT_PROFILE_VALIDATOR = new Validator(
    required = Some(List(C.ImportProfile.NAME)))

  override def add(obj: ImportProfile)(implicit txId: TransactionId = new TransactionId): JsObject = {
    IMPORT_PROFILE_VALIDATOR.validate(obj)
    super.add(obj)
  }

}
