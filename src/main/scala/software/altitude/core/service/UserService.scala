package software.altitude.core.service

import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.{Const, NotFoundException, Context, Altitude}
import software.altitude.core.dao.UserDao
import software.altitude.core.models.User
import software.altitude.core.transactions.{TransactionId, AbstractTransactionManager}
import net.codingwell.scalaguice.InjectorExtensions._

class UserService(val app: Altitude) extends BaseService[User] {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[UserDao]
  override protected val txManager = app.injector.instance[AbstractTransactionManager]

  override def getById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    throw new NotImplementedError
  }

  def getUserById(id: String)(implicit txId: TransactionId = new TransactionId): User = {
    txManager.asReadOnly[JsObject] {

      implicit val context = Context.EMPTY

      DAO.getById(id) match {
        case Some(obj) => obj
        case None => throw NotFoundException(s"Cannot find ID '$id'")
      }
    }
  }
}
