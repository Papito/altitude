package altitude.exceptions

import play.api.libs.json.JsObject

case class DuplicateException(objJson: JsObject, duplicateOf: JsObject) extends Exception
