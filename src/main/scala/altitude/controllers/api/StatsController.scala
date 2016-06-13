package altitude.controllers.api

import org.scalatra.Ok
import org.slf4j.LoggerFactory

import altitude.{Const => C}
import play.api.libs.json.Json

class StatsController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get(s"/") {
    Ok(Json.obj(
      C("Api.Stats.STATS") -> app.service.stats.getStats.stats.map(_.toJson)
    ))
  }

  get(s"/:${C("Stat.DIMENSION")}") {
    val dimension = params.get(C("Stat.DIMENSION")).get.toLowerCase
    val dimVal = app.service.stats.getStats.getStatValue(dimension)

    Ok(Json.obj(
      C("Stat.DIM_VAL") -> dimVal
    ))
  }
}
