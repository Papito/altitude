package software.altitude.core.models
import play.api.libs.json._
import software.altitude.core.util.Util

import java.time.LocalDateTime
import scala.language.implicitConversions

object BaseModel {
  // implicit converter to go from model to JSON
  implicit def toJson(obj: BaseModel): JsObject = obj.toJson
}

abstract class BaseModel {
  val id: Option[String]
  val createdAt: Option[LocalDateTime]
  val updatedAt: Option[LocalDateTime]

  // Should be always used to get the ID of an object, unless we are positive that the object has not been persisted yet
  def persistedId: String = {
    id match {
      case None => throw new RuntimeException("Cannot get persisted ID for a model that has not been saved yet")
      case _ => id.get
    }
  }

  // implicit converter to go from JSON to model
  implicit def toJson: JsObject

  /**
   * Returns core JSON attributes that all models should have
    */
  protected def coreJsonAttrs: JsObject = JsObject(Map(
    Field.ID -> {id match {
      case None => JsNull
      case _ => JsString(id.get)
    }},

    Field.CREATED_AT -> {createdAt match {
      case None => JsNull
      case _ => JsString(Util.localDateTimeToString(createdAt))
    }},

    Field.UPDATED_AT -> {updatedAt match {
      case None => JsNull
      case _ => JsString(Util.localDateTimeToString(updatedAt))
    }}
  ).toSeq)

  override def toString: String = toJson.toString()
}
