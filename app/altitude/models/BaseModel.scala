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

  protected var createdAt: Option[DateTime]  = None

  def createdAt_= (arg: DateTime): Unit = {
    if (createdAt.isDefined)
      throw new RuntimeException("Cannot set 'created_at' twice")
    createdAt = Some(arg)
  }

  protected var updatedAt: Option[DateTime]  = None

  def updatedAt_= (arg: DateTime): Unit = {
    if (updatedAt.isDefined)
      throw new RuntimeException("Cannot set 'updated_at' twice")
    updatedAt = Some(arg)
  }

  /*
  Every model must have these (but not necessarily with values)
   */
  protected def coreAttrs = JsObject(Map(
    C.Base.ID -> {if (id.isDefined) JsString(id.get) else JsNull},
    C.Base.CREATED_AT -> {
      val isoDateTimeStr: String = altitude.Util.isoDateTime(createdAt)
      isoDateTimeStr match {
        case "" => JsNull
        case _ => JsString(isoDateTimeStr)
      }
    },
    C.Base.UPDATED_AT -> {
      val isoDateTimeStr: String = altitude.Util.isoDateTime(updatedAt)
      isoDateTimeStr match {
        case "" => JsNull
        case _ => JsString(isoDateTimeStr)
      }
    }
  ).toSeq)

  def toJson: JsObject
}
