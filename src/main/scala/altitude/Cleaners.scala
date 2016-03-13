package altitude

import altitude.{Const => C}
import play.api.libs.json.{JsObject, Json}

object Cleaners {
  case class Cleaner(trim: Option[List[String]] = None,
                     lower:  Option[List[String]] = None,
                     defaults:  Option[Map[String, String]] = None) {

    def clean(json: JsObject): JsObject = {
      doLower(doDefaults(doTrim(json))) ++ Json.obj(C.Base.IS_CLEAN -> true)
    }

    protected def doTrim(json: JsObject): JsObject = {
      json ++ trim.getOrElse(List[String]()).foldLeft(Json.obj()) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.nonEmpty => res ++ Json.obj(field.toString -> v.get.trim)
        }
      }
    }

    protected def doLower(json: JsObject): JsObject = {
      json ++ lower.getOrElse(List[String]()).foldLeft(Json.obj()) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.nonEmpty => res ++ Json.obj(field.toString -> v.get.toLowerCase)
        }
      }
    }

    protected def doDefaults(json: JsObject): JsObject = {
      json ++ defaults.getOrElse(Map[String, String]()).keys.foldLeft(Json.obj()) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.isEmpty => res ++ Json.obj(
            field.toString -> defaults.get(field.toString) )
        }
      }
    }
  }
}
