package altitude.service

import altitude.Altitude
import altitude.dao.RepositoryDao
import altitude.exceptions.DuplicateException
import altitude.models.{User, Repository}
import altitude.models.search.{QueryResult, Query}
import altitude.transactions.{TransactionId, AbstractTransactionManager}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

class RepositoryService(app: Altitude) extends BaseService[Repository](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[RepositoryDao]
  override protected val txManager = app.injector.instance[AbstractTransactionManager]
}

