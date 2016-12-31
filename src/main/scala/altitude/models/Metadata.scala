package altitude.models

import scala.language.implicitConversions

import play.api.libs.json._

object Metadata {
  implicit def fromJson(json: JsObject): Metadata = {
    val data = json.keys.foldLeft(Map[String, Set[String]]()) { (res, fieldId) =>
      res + (fieldId -> (json \ fieldId).as[Set[String]])
    }

    new Metadata(data)
  }
}

class Metadata(private val data: Map[String, Set[String]] = Map[String, Set[String]]()) extends BaseModel with NoId {

  override val toJson = data.foldLeft(Json.obj()) { (res, m) =>
    val fieldName = m._1
    val values: JsArray = JsArray(m._2.toSeq.map { v =>
      Json.obj(fieldName -> JsString(v))
    })
    res ++ Json.obj(fieldName.toString -> values)
  }
}
