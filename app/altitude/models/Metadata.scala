package altitude.models

import play.api.libs.json.{JsValue, Json}

import scala.collection.immutable.HashMap
import scala.language.implicitConversions

object Metadata {
  implicit def toJson(obj: Metadata): JsValue = Json.obj(
    "id" -> obj.id
  )

  implicit def fromJson(json: JsValue): Metadata = new Metadata(
    raw = new HashMap[String, String]()
  )

}

case class Metadata(private val raw: Map[String, String]) extends BaseModel