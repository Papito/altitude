package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.dao.AssetDao
import software.altitude.core.models.Asset
import software.altitude.core.util.Query
import software.altitude.core.util.QueryResult
import software.altitude.core.{Const => C}

/**
 * This is a "dumb" DAO service for asset table.
 *
 * All the actual asset management logic is handled by the LibraryService exclusively.
 */
class AssetService(val app: Altitude) extends BaseService[Asset] {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val dao: AssetDao = app.injector.instance[AssetDao]

  def setRecycledProp(asset: Asset, isRecycled: Boolean): Unit = {

    if (asset.isRecycled == isRecycled) {
      return
    }

    txManager.withTransaction[Unit] {
      log.info(s"Setting asset [${asset.id.get}] recycled flag to [$isRecycled]")
      val updatedAsset: Asset  = asset.copy(isRecycled = isRecycled)
      dao.updateById(asset.id.get, data = updatedAsset, fields = List(C.Asset.IS_RECYCLED))
    }
  }

  override def query(q: Query): QueryResult = {
    dao.queryNotRecycled(q.withRepository())
  }

  def queryRecycled(q: Query): QueryResult = {
    dao.queryRecycled(q.withRepository())
  }

  def queryAll(q: Query): QueryResult = {
    dao.queryAll(q.withRepository())
  }

  // NO
  // Read the class description
}
