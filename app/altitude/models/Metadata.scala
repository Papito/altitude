package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}

import scala.collection.immutable.HashMap
import scala.language.implicitConversions

object Metadata {
  implicit def fromJson(json: JsValue): Metadata = try {
    new Metadata(
      id = (json \ "id").as[String]
    )} catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert from $json: ${e.getMessage}")
  }
}

case class Metadata(
  override final val id: String = BaseModel.genId,
  private val raw: Map[String, String] = new HashMap[String, String]())
  extends BaseModel(id) {
  override def toJson = Json.obj(
    "id" -> id
  )
}