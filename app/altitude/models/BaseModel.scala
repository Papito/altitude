package altitude.models

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import altitude.{Const => C}

import scala.language.implicitConversions

object BaseModel {
  final def genId: String = BSONObjectID.generate.stringify
  implicit def toJson(obj: BaseModel): JsObject = obj.toJson
}

abstract class BaseModel {
  val id: Option[String]

  def toJson: JsObject

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
  protected def coreJsonAttrs = JsObject(Map(
    C.Base.ID -> {if (id.isDefined) JsString(id.get) else JsNull},
    C.Base.CREATED_AT -> {
      createdAt match {
        case None => JsNull
        case _ => Json.obj("$date" -> createdAt)
      }
    },
    C.Base.UPDATED_AT -> {
      updatedAt match {
        case None => JsNull
        case _ => Json.obj("$date" -> updatedAt)
      }
    }
  ).toSeq)

}
