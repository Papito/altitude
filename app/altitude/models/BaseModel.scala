package altitude.models

import play.api.libs.json.JsObject
import reactivemongo.bson.BSONObjectID

abstract class BaseModel(objId: Option[String] = None, val isClean: Boolean = false) {
  val id: String = objId.getOrElse( this.genId )
  protected final def genId: String = BSONObjectID.generate.stringify

  override def toString = toJson.toString()
  def toJson: JsObject
}
