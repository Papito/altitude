package altitude.models

import altitude.{Const => C}
import play.api.libs.json._
import scala.language.implicitConversions


object Stats {
  val DIMENSIONS = Set(
    C("Stats.ALL_ASSETS"),
    C("Stats.ALL_ASSET_BYTES"),
    C("Stats.UNCATEGORIZED_ASSETS"),
    C("Stats.RECYCLED_ASSETS"),
    C("Stats.RECYCLED_BYTES"))

  implicit def fromJson(json: JsValue): Stats = Stats(stats = Map(
    C("Stats.ALL_ASSETS") -> (json \ C("Stats.ALL_ASSETS")).as[Int],
    C("Stats.ALL_ASSET_BYTES") -> (json \ C("Stats.ALL_ASSET_BYTES")).as[Int],
    C("Stats.UNCATEGORIZED_ASSETS") -> (json \ C("Stats.UNCATEGORIZED_ASSETS")).as[Int],
    C("Stats.RECYCLED_ASSETS") -> (json \ C("Stats.RECYCLED_ASSETS")).as[Int],
    C("Stats.RECYCLED_BYTES") -> (json \ C("Stats.RECYCLED_BYTES")).as[Int]
  ))

  implicit def toJson(stats: Stats): JsObject = stats.toJson

}

case class Stats(stats: Map[String, Int]) {

  def toJson = {
    Json.obj(
      C("Stats.ALL_ASSETS") -> stats.get(C("Stats.ALL_ASSETS")),
      C("Stats.ALL_ASSET_BYTES") -> stats.get(C("Stats.ALL_ASSET_BYTES")),
      C("Stats.UNCATEGORIZED_ASSETS") -> stats.get(C("Stats.UNCATEGORIZED_ASSETS")),
      C("Stats.RECYCLED_ASSETS") -> stats.get(C("Stats.RECYCLED_ASSETS")),
      C("Stats.RECYCLED_BYTES") -> stats.get(C("Stats.RECYCLED_BYTES"))
    )
  }
}

