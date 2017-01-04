package altitude.dao.mongo

import altitude.transactions.TransactionId
import altitude.{Const => C, Context, Altitude}
import altitude.models.Metadata
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, JsUndefined, JsObject, Json}

class AssetDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.AssetDao {
  private final val log = LoggerFactory.getLogger(getClass)

  /**
   * Set metadata for an asset. This does nothing special, and we simply use the
   * base <code>updateById</code> method to set the document property in Mongo
   */
  override def setMetadata(assetId: String, metadata: Metadata)
                          (implicit ctx: Context, txId: TransactionId) = {
    val updateObj = Json.obj(C.Asset.METADATA -> metadata.toJson)
    updateById(assetId, updateObj, List(C.Asset.METADATA))
  }

  private val METADATA_FIELDS_CONSTRAINT = MongoDBObject(C.Asset.METADATA -> 1)
  override def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata] = {
    log.debug(s"Getting metadata by ID '$assetId'", C.LogTag.DB)

    val q = MongoDBObject(
      "_id" -> assetId,
      C.Base.REPO_ID -> ctx.repo.id.get)

    val o: Option[DBObject] = COLLECTION.findOneByID(assetId, METADATA_FIELDS_CONSTRAINT)
    //val o: Option[DBObject] = COLLECTION.findOne(q, METADATA_FIELDS_CONSTRAINT)

    log.debug(s"RETRIEVED object: $o", C.LogTag.DB)

    o.isDefined match {
      // we have a record
      case true =>
        val json = Json.parse(o.get.toString).as[JsObject]

        json \ C.Asset.METADATA match {
          // found no metadata set
          case v: JsUndefined => None
          // found metadata field
          case metadataJson: JsValue =>
            val metadata: Metadata = metadataJson.asInstanceOf[JsObject]
            Some(metadata)
        }

      // we don't have a record
      case false => None
    }

  }
}