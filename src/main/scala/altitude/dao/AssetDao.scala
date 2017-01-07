package altitude.dao

import altitude.Context
import altitude.models.Metadata
import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory

trait AssetDao extends BaseDao {
  private final val log = LoggerFactory.getLogger(getClass)

  def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata]
  def setMetadata(assetId: String, metadata: Metadata)(implicit ctx: Context, txId: TransactionId)

  def updateMetadata(assetId: String, metadata: Metadata)
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
    val newMetadata = new Metadata(existingMetadata.data ++ metadata.data)
    log.debug(s"New metadata -> $newMetadata")

    setMetadata(assetId, newMetadata)
  }
}