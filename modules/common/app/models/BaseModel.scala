package models

import play.api.libs.json.{Json, JsObject}

abstract class BaseModel[ID](objId: Option[ID] = None, val isClean: Boolean = false) {
  val id: ID = objId.getOrElse( this.genId )
  def toJson: JsObject
  override def toString= toJson.toString()
  protected def genId: ID
}
