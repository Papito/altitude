package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.dao.StatDao
import software.altitude.core.models.{Asset, Stat, Stats}
import software.altitude.core.transactions.{AbstractTransactionManager, TransactionId}
import software.altitude.core.util.Query
import software.altitude.core.{Altitude, Context}

class StatsService(val app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO: StatDao = app.injector.instance[StatDao]
  protected val txManager: AbstractTransactionManager = app.injector.instance[AbstractTransactionManager]

  def getStats(implicit ctx: Context, txId: TransactionId = new TransactionId): Stats = {
    txManager.asReadOnly[Stats] {
      val stats: List[Stat] = DAO.query(Query()).records.map(Stat.fromJson)

      // Assemble the total stats on-the-fly
      val totalAssetsDims = Stats.SORTED_ASSETS :: Stats.RECYCLED_ASSETS :: Stats.TRIAGE_ASSETS :: Nil
      val totalAssets = stats.filter(stat => totalAssetsDims.contains(stat.dimension)).map(_.dimVal).sum

      val totalBytesDims = Stats.SORTED_BYTES :: Stats.RECYCLED_BYTES :: Stats.TRIAGE_BYTES :: Nil
      val totalBytes = stats.filter(stat => totalBytesDims.contains(stat.dimension)).map(_.dimVal).sum

      val wTotals = Stat(Stats.TOTAL_ASSETS, totalAssets) :: Stat(Stats.TOTAL_BYTES, totalBytes) :: stats

      Stats(wTotals)
    }
  }

  private def incrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context, txId: TransactionId): Unit = {
    DAO.incrementStat(statName, count)
  }

  private def decrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context, txId: TransactionId): Unit = {
    DAO.decrementStat(statName, count)
  }

  def createStat(dimension: String)
                (implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    txManager.withTransaction {
      val stat = Stat(dimension, 0)
      DAO.add(stat)
    }
  }

  def addAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Adding asset [${asset.id}]")

    /* To TRIAGE */
    if (app.service.folder.isTriageFolder(asset.folderId)) {
      log.debug(s"Asset [${asset.id}] moving TO triage. Incrementing TRIAGE")
      app.service.stats.incrementStat(Stats.TRIAGE_ASSETS)
      app.service.stats.incrementStat(Stats.TRIAGE_BYTES, asset.sizeBytes)
    }

    /* To SORTED */
    if (!app.service.folder.isSystemFolder(Some(asset.folderId))) {
      log.debug(s"Asset [${asset.id}] moving TO sorted. Incrementing SORTED")

      app.service.stats.incrementStat(Stats.SORTED_ASSETS)
      app.service.stats.incrementStat(Stats.SORTED_BYTES, asset.sizeBytes)

      log.debug(s"Incrementing folder counter for asset [${asset.id}]")
      app.service.folder.incrAssetCount(asset.folderId)
    }
  }

  def moveAsset(asset: Asset, destFolderId: String)(implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Moving asset [${asset.id}]")

    if (asset.isRecycled) {
      log.debug(s"Asset [${asset.id}] is recycled")
      moveRecycledAsset(asset, destFolderId)
      return
    }

    // short circuit for moving asset within the sorted hierarchy
    if (!app.service.folder.isSystemFolder(Some(asset.folderId)) &&
      !app.service.folder.isSystemFolder(Some(destFolderId))) {
      log.debug(s"Asset [${asset.id}] WITHIN sorted.")

      log.debug(s"Decrementing old folder counter for asset [${asset.id}]")
      app.service.folder.decrAssetCount(asset.folderId)
      log.debug(s"Incrementing new folder counter for asset [${asset.id}]")
      app.service.folder.incrAssetCount(destFolderId)
      return
    }

    /* From TRIAGE */
    if (app.service.folder.isTriageFolder(asset.folderId)) {
      log.debug(s"Asset [${asset.id}] moving FROM triage. Decrementing TRIAGE")
      app.service.stats.decrementStat(Stats.TRIAGE_ASSETS)
      app.service.stats.decrementStat(Stats.TRIAGE_BYTES, asset.sizeBytes)
    }

    /* From SORTED */
    if (!app.service.folder.isSystemFolder(Some(asset.folderId))) {
      log.debug(s"Asset [${asset.id}] moving FROM sorted. Decrementing SORTED")

      app.service.stats.decrementStat(Stats.SORTED_ASSETS)
      app.service.stats.decrementStat(Stats.SORTED_BYTES, asset.sizeBytes)

      log.debug(s"Decrementing folder counter for asset [${asset.id}]")
      app.service.folder.decrAssetCount(asset.folderId)
    }

    /* To TRIAGE */
    if (app.service.folder.isTriageFolder(destFolderId)) {
      log.debug(s"Asset [${asset.id}] moving TO triage. Incrementing TRIAGE")
      app.service.stats.incrementStat(Stats.TRIAGE_ASSETS)
      app.service.stats.incrementStat(Stats.TRIAGE_BYTES, asset.sizeBytes)
    }

    /* To SORTED */
    if (!app.service.folder.isSystemFolder(Some(destFolderId))) {
      log.debug(s"Asset [${asset.id}] moving TO sorted. Incrementing SORTED")

      app.service.stats.incrementStat(Stats.SORTED_ASSETS)
      app.service.stats.incrementStat(Stats.SORTED_BYTES, asset.sizeBytes)

      log.debug(s"Incrementing folder counter for asset [${asset.id}]")
      app.service.folder.incrAssetCount(destFolderId)
    }
  }

  private def moveRecycledAsset(asset: Asset, destFolderId: String)(implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Moving recycled asset [${asset.id}]. Decrementing RECYCLED")

    app.service.stats.decrementStat(Stats.RECYCLED_ASSETS)
    app.service.stats.decrementStat(Stats.RECYCLED_BYTES, asset.sizeBytes)

    if (app.service.folder.isTriageFolder(destFolderId)) {
      log.debug(s"Asset [${asset.id}] moving TO triage. Incrementing TRIAGE")
      app.service.stats.incrementStat(Stats.TRIAGE_ASSETS)
      app.service.stats.incrementStat(Stats.TRIAGE_BYTES, asset.sizeBytes)
    }
    else if(!app.service.folder.isSystemFolder(Some(destFolderId))) {
      log.debug(s"Recycled asset [${asset.id}] moving TO sorted. Incrementing SORTED")
      app.service.stats.incrementStat(Stats.SORTED_ASSETS)
      app.service.stats.incrementStat(Stats.SORTED_BYTES, asset.sizeBytes)

      log.debug(s"Incrementing folder counter for asset [${asset.id}]")
      app.service.folder.incrAssetCount(destFolderId)
    }
  }

  def recycleAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
    log.debug(s"Recycling asset [${asset.id}]. Incrementing RECYCLED")

    if (app.service.folder.isTriageFolder(asset.folderId)) {
      log.debug(s"Asset [${asset.id}] recycled and moving FROM triage. Decrementing TRIAGE")
      app.service.stats.decrementStat(Stats.TRIAGE_ASSETS)
      app.service.stats.decrementStat(Stats.TRIAGE_BYTES, asset.sizeBytes)
    }

    if (!app.service.folder.isSystemFolder(Some(asset.folderId))) {
      log.debug(s"Asset [${asset.id}] recycled and moving FROM sorted. Decrementing SORTED")
      app.service.stats.decrementStat(Stats.SORTED_ASSETS)
      app.service.stats.decrementStat(Stats.SORTED_BYTES, asset.sizeBytes)
    }

    app.service.stats.incrementStat(Stats.RECYCLED_ASSETS)
    app.service.stats.incrementStat(Stats.RECYCLED_BYTES, asset.sizeBytes)

    log.debug(s"Decrementing folder counter for recycled asset [${asset.id}]")
    app.service.folder.decrAssetCount(asset.folderId)
  }

  def restoreAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
    moveRecycledAsset(asset, asset.folderId)
  }

  def purgeAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId): Unit = {
    if (app.service.folder.isTriageFolder(asset.folderId)) {
      app.service.stats.decrementStat(Stats.RECYCLED_ASSETS)
      app.service.stats.decrementStat(Stats.RECYCLED_BYTES, asset.sizeBytes)
    }
  }

}

