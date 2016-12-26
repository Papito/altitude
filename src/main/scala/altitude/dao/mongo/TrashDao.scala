package altitude.dao.mongo

import altitude.models.Trash
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context, Util}
import com.mongodb.casbah.Imports._
import play.api.libs.json.{JsObject, Json}

class TrashDao(val app: Altitude)
  extends BaseMongoDao("trash") with altitude.dao.TrashDao {

  override protected def fixMongoFields(json: JsObject): JsObject = super.fixMongoFields(json) ++
    Json.obj(C.Trash.RECYCLED_AT ->  (json \ C.Trash.RECYCLED_AT \ "$date").asOpt[String])

  override protected def makeObjectForInsert(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): DBObject = {
    val trash: Trash = jsonIn
    super.makeObjectForInsert(jsonIn) ++ MongoDBObject(
      C.Trash.RECYCLED_AT -> Util.utcNowNoTZ,
      C.Base.CREATED_AT -> trash.createdAt,
      C.Base.UPDATED_AT -> trash.updatedAt)
  }
}