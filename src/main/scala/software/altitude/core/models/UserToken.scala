package software.altitude.core.models

import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import software.altitude.core.util.Util

import java.time.LocalDateTime
import scala.language.implicitConversions

object UserToken {
  implicit val reads: Reads[UserToken] = (json: JsValue) => {
    val expiresAtStr = (json \ Field.UserToken.EXPIRES_AT).as[String]
    JsSuccess(UserToken(
      userId = (json \ Field.UserToken.ACCOUNT_ID).as[String],
      token = (json \ Field.UserToken.TOKEN).as[String],
      expiresAt = Util.stringToLocalDateTime(expiresAtStr).get
    ))
  }

  implicit val writes: OWrites[UserToken] = (userToken: UserToken) => {
    Json.obj(
      Field.UserToken.ACCOUNT_ID -> userToken.userId,
      Field.UserToken.TOKEN -> userToken.token,
      Field.UserToken.EXPIRES_AT -> userToken.expiresAt.toString
    )
  }
  implicit def fromJson(json: JsValue): UserToken = Json.fromJson[UserToken](json).get
}

case class UserToken(userId: String,
                     token: String,
                     expiresAt: LocalDateTime) {

  val toJson: JsObject = Json.toJson(this).as[JsObject]
}
