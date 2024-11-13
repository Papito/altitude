package software.altitude.core.models

import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import software.altitude.core.models.AccountType.AccountType

import scala.language.implicitConversions

object User {
  implicit val reads: Reads[User] = (json: JsValue) => {
    JsSuccess(User(
      id = (json \ Field.ID).asOpt[String],
      email = (json \ Field.User.EMAIL).as[String],
      name = (json \ Field.User.NAME).as[String],
      accountType = (json \ Field.User.ACCOUNT_TYPE).as[AccountType],
      lastActiveRepoId = (json \ Field.User.LAST_ACTIVE_REPO_ID).asOpt[String]
    ))
  }

  implicit val writes: OWrites[User] = (user: User) => {
    Json.obj(
      Field.ID -> user.id,
      Field.User.EMAIL -> user.email,
      Field.User.NAME -> user.name,
      Field.User.ACCOUNT_TYPE -> user.accountType,
      Field.User.LAST_ACTIVE_REPO_ID -> user.lastActiveRepoId
    )
  }

  implicit def fromJson(json: JsValue): User = Json.fromJson[User](json).get
}

case class User(id: Option[String] = None,
                email: String,
                name: String,
                accountType: AccountType,
                lastActiveRepoId: Option[String] = None) extends BaseModel with NoDates {

  val toJson: JsObject = Json.toJson(this).as[JsObject]

  override def toString: String = s"<user> ${id.getOrElse("NO ID")}, email: $email, accountType: $accountType"

  def forgetMe(): Unit = {
    println("User: this is where you'd invalidate the saved token in you User model")
  }
}
