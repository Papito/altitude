package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import software.altitude.core._
import software.altitude.core.dao.UserDao
import software.altitude.core.models.User
import software.altitude.core.transactions.TransactionManager

class UserService(val app: Altitude) extends BaseService[User] {
  protected val dao: UserDao = app.injector.instance[UserDao]
  override protected val txManager: TransactionManager = app.txManager

  def switchContextToUser(repo: User): Unit = {
    RequestContext.account.value = Some(repo)
  }
}
