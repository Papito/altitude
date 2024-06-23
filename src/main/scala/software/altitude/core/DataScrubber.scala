package software.altitude.core

import play.api.libs.json.JsObject
import play.api.libs.json.Json

/**
 * Cleaners are used for data hygiene, before validation.
 */
/**
 * Base version of the cleaner.
 *
 * @param trim fields to trim leading and trailing spaces from
 * @param lower fields to lowercase
 * @param defaults fields to set defaults for, if they are not given
 */
case class DataScrubber(trim: List[String] = List(),
                        lower: List[String] = List(),
                        defaults: Map[String, String] = Map.empty) {

  def scrub(json: JsObject): JsObject = {
    val trimmed = doTrim(json)
    val wDefaults = doDefaults(trimmed)
    val lowerCased = doLower(wDefaults)
    lowerCased
  }

  private def doTrim(json: JsObject): JsObject = {
    json ++ trim.foldLeft(Json.obj()) { (res, field) =>
      (json \ field).asOpt[String] match {
        case v: Some[String] if v.nonEmpty => res ++ Json.obj(field -> v.get.trim)
        case _ => res
      }
    }
  }

  private def doLower(json: JsObject): JsObject = {
    json ++ lower.foldLeft(Json.obj()) { (res, field) =>
      (json \ field).asOpt[String] match {
        case v: Some[String] if v.nonEmpty => res ++ Json.obj(field -> v.get.toLowerCase)
        case _ => res
      }
    }
  }

  private def doDefaults(json: JsObject): JsObject = {
    json ++ defaults.keys.foldLeft(Json.obj()) { (res, field) =>
      (json \ field).asOpt[String] match {
        case v: Some[String] if v.isEmpty => res ++ Json.obj(
          field -> defaults.get(field) )
        case _ => res
      }
    }
  }
}
