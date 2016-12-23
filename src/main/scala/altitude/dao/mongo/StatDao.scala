package altitude.dao.mongo

import altitude.{Altitude, Const => C, Context}
import com.mongodb.casbah.Imports._

class StatDao(val app: Altitude) extends BaseMongoDao("altitude_stats") with altitude.dao.StatDao {

  def incrementStat(statName: String, count: Long = 1)(implicit ctx: Context): Unit = {
    val query: DBObject =  MongoDBObject(
      C.Base.REPO_ID -> ctx.repo.id.get,
      C.Stat.DIMENSION -> statName
    )
    val o: DBObject =  MongoDBObject(
      "$inc" -> MongoDBObject(C.Stat.DIM_VAL -> count)
    )

    COLLECTION.update(query, o)
  }
}
