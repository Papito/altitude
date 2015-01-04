package dao.manager

import dao.common.BaseDao
import models.common.Asset

import scala.concurrent.Future

trait LibraryDao extends BaseDao[Asset] {
  def add(asset: Asset): Future[Asset]
}
