package altitude.manager.dao

import altitude.common.dao.BaseDao
import altitude.common.models.Asset

import scala.concurrent.Future

trait LibraryDao extends BaseDao[Asset] {
  def add(asset: Asset): Future[Asset]
}
