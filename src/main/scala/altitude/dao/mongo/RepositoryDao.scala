package altitude.dao.mongo

import altitude.{Altitude, Const => C, Context}
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

class RepositoryDao(val app: Altitude) extends BaseMongoDao("repositories") with altitude.dao.RepositoryDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override def getById(id: String)(implicit ctx: Context): Option[JsObject] = {
    log.debug(s"Getting repository by ID '$id'", C.LogTag.DB)

    val o: Option[DBObject] = COLLECTION.findOneByID(id)

    log.debug(s"RETRIEVED object: $o", C.LogTag.DB)

    o.isDefined match {
      case false => None
      case true =>
        val json = Json.parse(o.get.toString).as[JsObject]
        Some(fixMongoFields(json))
    }
  }

  override protected def makeObjectForInsert(jsonIn: JsObject)(implicit ctx: Context): DBObject = {
    val o = super.makeObjectForInsert(jsonIn)
    // this is the only model that does not need the repository ID
    o.removeField(C.Base.REPO_ID)
    o
  }
}
