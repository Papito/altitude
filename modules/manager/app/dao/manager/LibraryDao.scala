package dao.manager

import dao.BaseDao
import models.Asset

import scala.concurrent.Future

trait LibraryDao extends BaseDao[Asset] {
  def add(asset: Asset): Future[Asset]
}
