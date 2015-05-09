package altitude.dao.mongo

import altitude.Util.log
import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.{Const => C, Util}
import org.joda.time.DateTime
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BaseMongoDao {
}

abstract class BaseMongoDao(private val collectionName: String) extends BaseDao {
  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    throw new NotImplementedError()
  }

  override def getById(id: String)(implicit txId: TransactionId): Option[JsObject] = {
    throw new NotImplementedError()
  }

  override def query(query: Query)(implicit txId: TransactionId): List[JsObject] = {
    throw new NotImplementedError()
  }

  /*
  Return a JSON record with timestamp and ID fields translated from Mongo's "extended" format
   */
  protected def fixMongoFields(json: JsObject): JsObject = {
    val createdAtMillis: Option[Long] =  (json \ C.Base.CREATED_AT \ "$date").asOpt[Long]
    val createdAt: Option[DateTime] = {
      if (createdAtMillis.isDefined) Some(new DateTime(createdAtMillis.get)) else None
    }

    val updatedAtMillis: Option[Long] =  (json \ C.Base.UPDATED_AT \ "$date").asOpt[Long]
    val updatedAt: Option[DateTime] = {
      if (updatedAtMillis.isDefined) Some(new DateTime(updatedAtMillis.get)) else None
    }

    json ++ Json.obj(
      C.Base.ID -> (json \ "_id" \ "$oid").as[String],
      C.Base.CREATED_AT -> {createdAt match {
        case None => JsNull
        case _ => Util.isoDateTime(createdAt)
      }},
      C.Base.UPDATED_AT -> {updatedAt match {
        case None => JsNull
        case _ => Util.isoDateTime(updatedAt)
      }}
    )
  }
}