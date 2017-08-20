package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.dao.RepositoryDao
import software.altitude.core.models.Repository
import software.altitude.core.transactions.AbstractTransactionManager

class RepositoryService(val app: Altitude) extends BaseService[Repository] {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[RepositoryDao]
  override protected val txManager = app.injector.instance[AbstractTransactionManager]
}

