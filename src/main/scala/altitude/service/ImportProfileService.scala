package altitude.service
//import altitude.models.ImportProfile
import altitude.{Const => C}

/*
object ImportProfileService {
  class ImportProfileValidator
    extends Validator(
      required = Some(List(C.ImportProfile.NAME, C.ImportProfile.TAG_DATA))) {

    override def validate(json: JsObject, raise: Boolean = true): ValidationException = {
      val ex: ValidationException = super.validate(json, raise = false)

      val tagData = json \ C.ImportProfile.TAG_DATA

      tagData match {
        case JsArray(elements) if elements.nonEmpty =>
        case JsArray(elements) if elements.isEmpty => ex.errors += (C.ImportProfile.TAG_DATA -> C.MSG("err.required"))
        case _ => ex.errors += (C.ImportProfile.TAG_DATA -> C.MSG("err.required"))
      }

      if (raise && ex.errors.nonEmpty) throw ex
      ex
    }
  }
}

class ImportProfileService(app: Altitude) extends BaseService[ImportProfile](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[ImportProfileDao]


  override val VALIDATOR = Some(new ImportProfileService.ImportProfileValidator)
  override val CLEANER = Some(Cleaners.Cleaner(trim = Some(List(C.ImportProfile.NAME))))
}
*/
