package altitude.models

import altitude.exceptions.FormatException
import play.api.libs.json.{JsResultException, JsValue, Json}

import scala.collection.immutable.HashMap
import scala.language.implicitConversions

object Metadata {
  implicit def toJson(obj: Metadata): JsValue = Json.obj(
    "id" -> obj.id
  )

  implicit def fromJson(json: JsValue): Metadata = try {
    new Metadata(
      raw = new HashMap[String, String]()
    )} catch {
    case e: JsResultException => throw new FormatException(s"Cannot convert to Asset from $json")
  }

}

case class Metadata(private val raw: Map[String, String]) extends BaseModel