package altitude.dao.mongo

import altitude.models.Metadata
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, JsUndefined, JsValue, Json}

class AssetDao(val app: Altitude) extends BaseMongoDao("assets") with altitude.dao.AssetDao {
  private final val log = LoggerFactory.getLogger(getClass)

  // this tells mongo to ignore all other fields when returning an asset over the wire
  private val METADATA_FIELDS_CONSTRAINT = MongoDBObject(C.Asset.METADATA -> 1)

  override def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata] = {
    log.debug(s"Getting metadata by ID '$assetId'", C.LogTag.DB)

    val o: Option[DBObject] = COLLECTION.findOneByID(assetId, METADATA_FIELDS_CONSTRAINT)

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

  /**
   * Set metadata for an asset. This does nothing special, and we simply use the
   * base <code>updateById</code> method to set the document property in Mongo
   */
  override def setMetadata(assetId: String, metadata: Metadata)
                          (implicit ctx: Context, txId: TransactionId) = {
    val updateObj = Json.obj(C.Asset.METADATA -> metadata.toJson)
    updateById(assetId, updateObj, List(C.Asset.METADATA))
  }
}