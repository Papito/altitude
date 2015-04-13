package altitude.models

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import altitude.{Const => C}

import scala.language.implicitConversions

object BaseModel {
  implicit def toJson(obj: BaseModel): JsObject = obj.toJson

  final def genId: String = BSONObjectID.generate.stringify

  // the run-around, to make changing these values less trivial
  def setCreatedAt(dt: DateTime, m: BaseModel): Unit = m.createdAt = Some(dt)
  def setUpdatedAt(dt: DateTime, m: BaseModel): Unit = m.updatedAt = Some(dt)
}

abstract class BaseModel(val id: Option[String]) {
  protected var createdAt: Option[DateTime]  = None
  protected var updatedAt: Option[DateTime]  = None

  protected def coreAttrs = JsObject(Map(
    C.Base.ID -> {if (id.isDefined) JsString(id.get) else JsNull},
    C.Base.CREATED_AT -> {
      altitude.Util.isoDateTime(createdAt) match {
        case "" => JsNull
        case _ => JsString(altitude.Util.isoDateTime(createdAt))
      }
    },
    C.Base.UPDATED_AT -> {
      altitude.Util.isoDateTime(updatedAt) match {
        case "" => JsNull
        case _ => JsString(altitude.Util.isoDateTime(updatedAt))
      }
    }
  ).toSeq)

  def toJson: JsObject
}
