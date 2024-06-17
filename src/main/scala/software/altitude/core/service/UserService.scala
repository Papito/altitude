package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsObject
import software.altitude.core._
import software.altitude.core.dao.UserDao
import software.altitude.core.models.User
import software.altitude.core.transactions.TransactionManager

class UserService(val app: Altitude) extends BaseService[User] {
  protected val dao: UserDao = app.injector.instance[UserDao]
  override protected val txManager: TransactionManager = app.txManager

  override def getById(id: String): JsObject = {
    throw new NotImplementedError
  }

  def getUserById(id: String): User = {
    txManager.asReadOnly[JsObject] {
      dao.getById(id) match {
        case Some(obj) => obj
        case None => throw NotFoundException(s"Cannot find ID '$id'")
      }
    }
  }

  def addUser(user: User): JsObject = {
    txManager.withTransaction[JsObject] {
      super.add(user)
    }
  }
}
