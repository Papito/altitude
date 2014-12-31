package models

import play.api.libs.json.{Json, JsObject}

abstract class BaseModel(val id: Option[String] = None, val isClean: Boolean = false) {
  def toJson: JsObject = Json.obj()
}
