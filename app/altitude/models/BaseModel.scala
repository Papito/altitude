package altitude.models

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import altitude.{Const => C}

import scala.language.implicitConversions

object BaseModel {
  implicit def toJson(obj: BaseModel): JsObject = obj.toJson

  final def genId: String = BSONObjectID.generate.stringify
}

abstract class BaseModel {
  val id: Option[String]
  def toJson: JsObject

  private var _createdAt: Option[DateTime]  = None
  def createdAt = _createdAt

  def createdAt_= (arg: DateTime): Unit = {
    if (_createdAt.isDefined)
      throw new RuntimeException("Cannot set 'created_at' twice")
    _createdAt = Some(arg)
  }

  private var _updatedAt: Option[DateTime]  = None
  def updatedAt = _updatedAt

  def updatedAt_= (arg: DateTime): Unit = {
    if (_updatedAt.isDefined)
      throw new RuntimeException("Cannot set 'updated_at' twice")
    _updatedAt = Some(arg)
  }

  /*
  Every model must have these (but not necessarily with values)
   */
  protected def coreAttrs = JsObject(Map(
    C.Base.ID -> {if (id.isDefined) JsString(id.get) else JsNull},
    C.Base.CREATED_AT -> {
      val isoDateTimeStr: String = altitude.Util.isoDateTime(_createdAt)
      isoDateTimeStr match {
        case "" => JsNull
        case _ => JsString(isoDateTimeStr)
      }
    },
    C.Base.UPDATED_AT -> {
      val isoDateTimeStr: String = altitude.Util.isoDateTime(_updatedAt)
      isoDateTimeStr match {
        case "" => JsNull
        case _ => JsString(isoDateTimeStr)
      }
    }
  ).toSeq)
}
