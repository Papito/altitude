package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}

import scala.collection.immutable.HashMap
import scala.language.implicitConversions

object Metadata {
  implicit def fromJson(json: JsValue): Metadata = try {
    new Metadata(
      objId = (json \ "id").asOpt[String],
      raw = new HashMap[String, String]()
    )} catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert to Asset from $json: ${e.getMessage}")
  }
}

case class Metadata(objId: Option[String] = None, private val raw: Map[String, String]) extends BaseModel(objId) {
  override def toJson = Json.obj(
    "id" -> id
  )
}