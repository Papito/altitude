package altitude.dao

import altitude.Context
import altitude.models.search.{QueryResult, Query}
import altitude.models.{Asset, Metadata}
import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

trait AssetDao extends BaseDao {
  private final val log = LoggerFactory.getLogger(getClass)

  def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata]

  def setMetadata(assetId: String, metadata: Metadata)(implicit ctx: Context, txId: TransactionId)

  def setAssetAsRecycled(assetId: String, isRecycled: Boolean)(implicit ctx: Context, txId: TransactionId)

  def queryNotRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult

  def queryRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult

  override def query(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult =
    throw new NotImplementedError("Can only directly query recycled and not recycled data sets")

  override def getAll(implicit ctx: Context, txId: TransactionId): List[JsObject] =
    throw new NotImplementedError

  def updateMetadata(assetId: String, metadata: Metadata, deletedFields: Set[String])
                             (implicit ctx: Context, txId: TransactionId) = {
    /**
     * Pedestrian version of this just overwrites fields for old metadata and re-sets it on the asset.
     * A better implementation - for advanced engines - updates only the metadata fields of interest.
     */
    val existingMetadata = getMetadata(assetId) match {
      case Some(m) => m
      case None => new Metadata
    }

    log.debug(s"Updating $existingMetadata with $metadata")
    val newData = (existingMetadata.data ++ metadata.data).filterNot(m => deletedFields.contains(m._1))
    val newMetadata = new Metadata(newData)
    log.debug(s"New metadata -> $newMetadata")

    setMetadata(assetId, newMetadata)
  }
}