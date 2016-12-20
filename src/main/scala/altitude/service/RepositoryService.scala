package altitude.service

import altitude.Altitude
import altitude.dao.RepositoryDao
import altitude.models.Repository
import altitude.transactions.AbstractTransactionManager
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class RepositoryService(app: Altitude) extends BaseService[Repository](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[RepositoryDao]
  override protected val txManager = app.injector.instance[AbstractTransactionManager]
}

