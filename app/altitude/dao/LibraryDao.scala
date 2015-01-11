package altitude.dao

import altitude.models.Asset

import scala.concurrent.Future

trait LibraryDao extends BaseDao[Asset] {
  def add(asset: Asset): Future[Asset]
}
