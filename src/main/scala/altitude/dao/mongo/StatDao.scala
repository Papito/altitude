package altitude.dao.mongo

import altitude.models.User
import altitude.{Const => C, Altitude}
import altitude.transactions.TransactionId
import com.mongodb.casbah.Imports._

class StatDao(val app: Altitude) extends BaseMongoDao("altitude_stats") with altitude.dao.StatDao {

  def incrementStat(statName: String, count: Long = 1)(implicit user: User, txId: TransactionId): Unit = {
    val query: DBObject =  MongoDBObject(
      C("Stat.DIMENSION") -> statName,
      C("Base.USER_ID") -> user.id.get
    )
    val o: DBObject =  MongoDBObject(
      "$inc" -> MongoDBObject(C("Stat.DIM_VAL") -> count)
    )

    COLLECTION.update(query, o)
  }
}
