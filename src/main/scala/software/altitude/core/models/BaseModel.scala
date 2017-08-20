package software.altitude.core.models

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import software.altitude.core.{Const => C, Util}

import scala.language.implicitConversions

object BaseModel {
  final val ID_LEN = 24

  // make a new model ID
  final def genId: String = scala.util.Random.alphanumeric.take(ID_LEN).mkString.toLowerCase

  // implicit converter to go from a model to JSON
  implicit def toJson(obj: BaseModel): JsObject = obj.toJson
}

abstract class BaseModel {
  val id: Option[String]

  def toJson: JsObject

  // created at - mutable, but can only be set once
  protected var _createdAt: Option[DateTime] = None

  def createdAt = _createdAt

  def createdAt_= (arg: DateTime): Unit = {
    if (_createdAt.isDefined)
      throw new RuntimeException("Cannot set 'created at' twice")
    _createdAt = Some(arg)
  }

  // updated at - mutable, but can only be set once
  protected var _updatedAt: Option[DateTime] = None

  def updatedAt = _updatedAt

  def updatedAt_= (arg: DateTime): Unit = {
    if (_updatedAt.isDefined)
      throw new RuntimeException("Cannot set 'updated at' twice")
    _updatedAt = Some(arg)
  }

  // is clean (for validation)
  protected var _isClean = false

  def isClean = _isClean

  def isClean_= (arg: Boolean): Boolean = {
    // if object is not clean, set as clean, otherwise, move along
    if (!_isClean && arg) {
      _isClean = true
    }

    isClean
  }

  /**
   * Returns core JSON attributes that every model should have
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

  /**
   * Return this type of object, but with core attributes
   * present, parsed from the passed in JSON object (if the values are present)
   */
  protected def withCoreAttr(json: JsValue): this.type  = {
    val isoCreatedAt = (json \ C.Base.CREATED_AT).asOpt[String]
    if (isoCreatedAt.isDefined) {
      createdAt = ISODateTimeFormat.dateTime().parseDateTime(isoCreatedAt.get)
    }

    val isoUpdatedAt = (json \ C.Base.UPDATED_AT).asOpt[String]
    if (isoUpdatedAt.isDefined) {
      updatedAt = ISODateTimeFormat.dateTime().parseDateTime(isoUpdatedAt.get)
    }

    this
  }

  override def toString = toJson.toString()
}
