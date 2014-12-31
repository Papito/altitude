package models

import play.api.libs.json.{Json, JsObject}

abstract class BaseModel[ID](val id: Option[ID] = None, val isClean: Boolean = false) {
  def toJson: JsObject = Json.obj()
}
