package altitude.models

import altitude.{Const => C, Util}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

import scala.language.implicitConversions

object BaseModel {
  //FIXME: alphanumeric
  final def genId: String = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE).toString
  implicit def toJson(obj: BaseModel): JsObject = obj.toJson
}

abstract class BaseModel {
  val id: Option[String]

  def toJson: JsObject

  // created at
  protected var _createdAt: Option[DateTime]  = None

  def createdAt: Option[DateTime] = _createdAt

  def createdAt_= (arg: DateTime): Unit = {
    if (!_createdAt.isDefined)
      throw new RuntimeException("Cannot set 'created_at' twice")
    _createdAt = Some(arg)
  }

  // updated at
  protected var _updatedAt: Option[DateTime]  = None

  def updatedAt: Option[DateTime] = _updatedAt

  def updatedAt_= (arg: DateTime): Unit = {
    if (!_updatedAt.isDefined)
      throw new RuntimeException("Cannot set 'updated_at' twice")
    _updatedAt = Some(arg)
  }

  /*
  Create core JSON attributes that every model should have
   */
  protected def coreJsonAttrs = JsObject(Map(
    C.Base.ID -> {id match {
      case None => JsNull
      case _ => JsString(id.get)
    }},

    C.Base.CREATED_AT -> {createdAt match {
        case None => JsNull
        case _ => JsString(Util.isoDateTime(createdAt))
    }},

    C.Base.UPDATED_AT -> {updatedAt match {
        case None => JsNull
        case _ => JsString(Util.isoDateTime(updatedAt))
    }}
  ).toSeq)

  // pull in core model attributes from JSON
  protected def withCoreAttr(json: JsValue): this.type  = {
    val isoCreatedAt = (json \ C.Base.CREATED_AT).asOpt[String]
    if (isoCreatedAt != None) {
      createdAt = ISODateTimeFormat.dateTime().parseDateTime(isoCreatedAt.get)
    }
    val isoUpdatedAt = (json \ C.Base.UPDATED_AT).asOpt[String]

    if (isoUpdatedAt != None) {
      updatedAt = ISODateTimeFormat.dateTime().parseDateTime(isoUpdatedAt.get)
    }

    this
  }
}
