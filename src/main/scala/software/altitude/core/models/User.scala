package software.altitude.core.models

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{JsObject, JsValue, Json, JsonConfiguration, OWrites, Reads}
import software.altitude.core.models.AccountType.AccountType

import scala.language.implicitConversions

object User {
  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit val writes: OWrites[User] = Json.writes[User]
  implicit val reads: Reads[User] = Json.reads[User]
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
