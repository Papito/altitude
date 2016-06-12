package altitude.dao.mongo

import altitude.{Const => C, Altitude}
import altitude.transactions.TransactionId
import com.mongodb.casbah.Imports._

class StatDao(val app: Altitude) extends BaseMongoDao("altitude_stats") with altitude.dao.StatDao {

  def incrementStat(statName: String, count: Int = 1)(implicit txId: TransactionId): Unit = {
    val query: DBObject =  MongoDBObject(
      C("Stat.DIMENSION") -> statName
    )
    val o: DBObject =  MongoDBObject(
      "$inc" -> MongoDBObject(C("Stat.DIM_VAL") -> count)
    )

    COLLECTION.update(query, o)
  }
}
