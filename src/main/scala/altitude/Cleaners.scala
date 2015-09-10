package altitude

import play.api.libs.json.{JsObject, Json}

object Cleaners {
  case class Cleaner(trim: Option[List[String]] = None,
                     lower:  Option[List[String]] = None,
                     defaults:  Option[Map[String, Short]] = None,
                     maxLenghts:  Option[Map[String, Short]] = None) {

    def clean(json: JsObject): JsObject = {
      doLower(maxLength(doDefaults(doTrim(json))))
    }

    protected def doTrim(json: JsObject): JsObject = {
      trim.getOrElse(List[String]()).fold(json) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.nonEmpty => json ++ Json.obj(field.toString -> v.get.trim)
          case _ => json
        }
      }.asInstanceOf[JsObject]
    }

    protected def doLower(json: JsObject): JsObject = {
      if (lower.isDefined) return json
      json
    }

    protected def doDefaults(json: JsObject): JsObject = {
      if (defaults.isDefined) return json
      json
    }
    protected def maxLength(obj: JsObject): JsObject = {
      if (maxLenghts.isDefined) return obj
      obj
    }

  }
}
