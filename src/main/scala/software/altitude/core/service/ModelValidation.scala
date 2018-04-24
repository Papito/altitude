package software.altitude.core.service

import play.api.libs.json.JsObject
import software.altitude.core.Cleaners.Cleaner
import software.altitude.core.Validators.ModelDataValidator

trait ModelValidation {
  // object cleaner
  protected val cleaner: Option[Cleaner] = None
  // object validator, invoked AFTER the cleaner
  protected val validator: Option[ModelDataValidator] = None

  /**
   * Use the service-wide definitions of data cleaner and validator
   *
   * @param objIn object to clean and validate
   *
   * @return copy of the original document, cleaned and validated, if any of those steps are defined
   */
  protected def cleanAndValidate(objIn: JsObject): JsObject = {
    val cleaned = cleaner match {
      case Some(cl) => cl.clean(objIn)
      case None => objIn
    }

    validator match {
      case Some(v) =>
        v.validate(cleaned)
        cleaned
      case None => cleaned
    }
  }

  /**
   * Use specific instances of data cleaner and validator
   *
   * @param objIn object to clean and validate
   * @param cleaner cleaner instance
   * @param validator validator instance
   *
   * @return copy of the original document, cleaned and validated, if any of those steps are defined
   */
  protected def cleanAndValidate(objIn: JsObject,
                                 cleaner: Option[Cleaner],
                                 validator: Option[ModelDataValidator]): JsObject = {
    val cleaned = cleaner match {
      case Some(x) => x.clean(objIn)
      case None => objIn
    }

    validator match {
      case Some(x) =>
        x.validate(cleaned)
        cleaned
      case None => cleaned
    }
  }
}
