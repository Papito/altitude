package software.altitude.core.service
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.AltitudeServletContext
import software.altitude.core._
import software.altitude.core.dao.UserDao
import software.altitude.core.dao.UserTokenDao
import software.altitude.core.models.User
import software.altitude.core.models.UserToken
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.{Query, Util}

import java.time.LocalDateTime

class UserService(val app: Altitude) extends BaseService[User] {
  protected val dao: UserDao = app.DAO.user
  private val tokenDao: UserTokenDao = app.DAO.userToken

  override protected val txManager: TransactionManager = app.txManager

  def switchContextToUser(user: User): Unit = {
    RequestContext.account.value = Some(user)
  }

  def loginAndGetUser(email: String, password: String): Option[User] = {
    txManager.withTransaction {
      val passwordHash = getPasswordHashByEmail(email)

      if (!Util.checkPassword(password, passwordHash)) {
        return None
      }

      val user: User = getByEmail(email)

      // save the token
      val userToken = UserToken(
        userId = user.persistedId,
        token = Util.randomStr(64),
        expiresAt = LocalDateTime.now.plusDays(Const.Security.MEMBER_ME_COOKIE_EXPIRATION_DAYS))

      tokenDao.add(userToken.toJson)
      AltitudeServletContext.usersByToken += (userToken.token -> user)

      switchContextToUser(user)
      Some(user)
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

  def getByToken(token: String): Option[User] = {
    if (AltitudeServletContext.usersByToken.contains(token)) {
      return Some(AltitudeServletContext.usersByToken(token))
    }

    None
  }

  def deleteToken(token: String): Unit = {
    logger.info("Deleting token: " + token)
    tokenDao.deleteByQuery(new Query(params = Map(Const.UserToken.TOKEN -> token)))
    AltitudeServletContext.usersByToken -= token
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

  def setLastActiveRepoId(user: User, repoId: String): Unit = {
    val updatedUserCopy = user.copy(
      lastActiveRepoId = Some(repoId)
    )
    txManager.withTransaction {
      dao.updateById(user.persistedId, data=updatedUserCopy, List(Const.User.LAST_ACTIVE_REPO_ID))
    }
  }

}
