package altitude.service

import altitude.Validators.Validator
import altitude.{Const => C, Cleaners, Altitude}
import altitude.dao.ImportProfileDao
import altitude.models.ImportProfile
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class ImportProfileService(app: Altitude) extends BaseService[ImportProfile](app) {
  val log =  LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[ImportProfileDao]

  override val VALIDATOR = Some(Validator(required = Some(List(C.ImportProfile.NAME))))
  //FIXME: custom validator must enforce tagData JSON structure (and lookout for JsNull as well)
  override val CLEANER = Some(Cleaners.Cleaner(trim = Some(List(C.ImportProfile.NAME))))
}
