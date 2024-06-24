package software.altitude.core.service

import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.{JsObject, Json}
import software.altitude.core._
import software.altitude.core.dao.UserDao
import software.altitude.core.models.User
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.Query

class UserService(val app: Altitude) extends BaseService[User] {
  protected val dao: UserDao = app.injector.instance[UserDao]
  override protected val txManager: TransactionManager = app.txManager

  def switchContextToUser(repo: User): Unit = {
    RequestContext.account.value = Some(repo)
  }

  def checkUserLogin(email: String, password: String): Boolean = {
    /*
    Since the user model does not explicitly store the hashed password,
    we need to do a low-level query to get the password hash
     */
    val query = new Query(params = Map(Const.User.EMAIL -> email))
    val sqlQuery = dao.sqlQueryBuilder.buildSelectSql(query)
    val userRec: Map[String, AnyRef] = dao.getOneRawRecordBySql(sqlQuery.sqlAsString, sqlQuery.bindValues)

    val passwordHash = userRec(Const.User.PASSWORD_HASH).asInstanceOf[String]
    Util.checkPassword(password, passwordHash)
  }

  override def add(objIn: User, queryForDup: Option[Query] = None): JsObject =
    throw new NotImplementedError("Use the alternate add() method with password")

  def add(objIn: User, password: String): JsObject = {
    // password and hash are not stored in the model and are not passed around outside of login flow
    val passwordHash = Util.hashPassword(password)
    dao.add(objIn.toJson ++ Json.obj(Const.User.PASSWORD_HASH -> passwordHash))
  }

}
