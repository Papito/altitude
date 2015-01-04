package dao.common

import scala.concurrent.Future

trait BaseDao[Model] {
  def add(model: Model): Future[Model]
}
