package altitude.models

import play.api.libs.json.{Json, JsValue, JsObject}
import reactivemongo.bson.BSONObjectID
import scala.language.implicitConversions

object BaseModel {
  implicit def toJson(obj: BaseModel): JsValue = Json.obj(
    "id" -> obj.id
  )
}

abstract class BaseModel(objId: Option[String] = None, val isClean: Boolean = false) {
  val id: String = objId.getOrElse( this.genId )
  protected final def genId: String = BSONObjectID.generate.stringify
  def toJson: JsValue = this
}
