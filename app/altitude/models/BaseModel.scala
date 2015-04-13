package altitude.models

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import reactivemongo.bson.BSONObjectID
import altitude.{Const => C}

import scala.language.implicitConversions

object BaseModel {
  final def genId: String = BSONObjectID.generate.stringify
  implicit def toJson(obj: BaseModel): JsValue = obj.toJson

  // the run-around, to make changing these values less trivial
  def setCreatedAt(dt: DateTime, m: BaseModel): Unit = m.createdAt = Some(dt)
  def setUpdatedAt(dt: DateTime, m: BaseModel): Unit = m.updatedAt = Some(dt)
}

abstract class BaseModel(id: String = BaseModel.genId) {
  protected var createdAt: Option[DateTime]  = None
  protected var updatedAt: Option[DateTime]  = None

  protected def coreAttrs = JsObject(Map(
    C.Base.ID -> JsString(id),
    C.Base.CREATED_AT -> JsString(altitude.Util.isoDateTime(createdAt)),
    C.Base.UPDATED_AT -> JsString(altitude.Util.isoDateTime(updatedAt))
  ).toSeq)

  def toJson: JsValue
}
