package dao.manager

import models.Asset
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

trait LibraryDao {
  def add(asset: Asset): Future[LastError]
}
