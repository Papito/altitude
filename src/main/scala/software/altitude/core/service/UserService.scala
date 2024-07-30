package software.altitude.core.service
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core._
import software.altitude.core.dao.UserDao
import software.altitude.core.models.User
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.Query
import software.altitude.core.AltitudeServletContext

class UserService(val app: Altitude) extends BaseService[User] {
  protected val dao: UserDao = app.DAO.user

  override protected val txManager: TransactionManager = app.txManager

  def switchContextToUser(repo: User): Unit = {
    RequestContext.account.value = Some(repo)
  }

  def loginAndGetUser(email: String, password: String): Option[User] = {
    txManager.asReadOnly {
      val passwordHash = getPasswordHashByEmail(email)

      if (Util.checkPassword(password, passwordHash)) {
        val user: User = getByEmail(email)
        switchContextToUser(user)
        Some(user)
      } else {
        None
      }
    }
  }

  override def add(objIn: User, queryForDup: Option[Query] = None): JsObject =
    throw new NotImplementedError("Use the alternate add() method with password")

  def add(objIn: User, password: String): JsObject = {
    // password and hash are not stored in the model and are not passed around outside of login flow
    val passwordHash = Util.hashPassword(password)
    dao.add(objIn.toJson ++ Json.obj(Const.User.PASSWORD_HASH -> passwordHash))
  }

  private def getPasswordHashByEmail(email: String): String = {
    // try cache first
    if (AltitudeServletContext.usersPasswordHashByEmail.contains(email)) {
      return AltitudeServletContext.usersPasswordHashByEmail(email)
    }

    val query = new Query(params = Map(Const.User.EMAIL -> email))
    val sqlQuery = dao.sqlQueryBuilder.buildSelectSql(query)

    /* Since the user model does not explicitly store the hashed password,
       we need to do a low-level query to get the password hash */
    val userRec: Map[String, AnyRef] = dao.getOneRawRecordBySql(sqlQuery.sqlAsString, sqlQuery.bindValues)

    val passwordHash = userRec(Const.User.PASSWORD_HASH).asInstanceOf[String]

    AltitudeServletContext.usersPasswordHashByEmail += (email -> passwordHash)
    passwordHash
  }

  private def getByEmail(email: String): JsObject = {
    // try cache first
    if (AltitudeServletContext.usersByEmail.contains(email)) {
      return AltitudeServletContext.usersByEmail(email).toJson
    }

    val query = new Query(params = Map(Const.User.EMAIL -> email))

    /* Since the user model does not explicitly store the hashed password,
       we need to do a low-level query to get the password hash */
    val user: User = dao.getOneByQuery(query)

    AltitudeServletContext.usersByEmail += (email -> user)
    user
  }

  override def getById(id: String): JsObject = {
    // try cache first
    if (AltitudeServletContext.usersById.contains(id)) {
      return AltitudeServletContext.usersById(id).toJson
    }

    val user = super.getById(id)
    AltitudeServletContext.usersById += (id -> user)
    user
  }

  def setActiveRepoId(user: User, repoId: String): Unit = {
    val updatedUserCopy = user.copy(
      activeRepoId = Some(repoId)
    )
    txManager.withTransaction {
      dao.updateById(user.persistedId, data=updatedUserCopy, List(Const.User.ACTIVE_REPO_ID))
    }
  }
}
