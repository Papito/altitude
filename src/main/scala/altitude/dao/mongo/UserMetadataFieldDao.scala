package altitude.dao.mongo

import altitude.exceptions.NotFoundException
import altitude.{Altitude, Const => C}
import altitude.models.{UserMetadataField, User}
import altitude.transactions.TransactionId
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject


class UserMetadataFieldDao(val app: Altitude) extends BaseMongoDao("metadata_fields") with altitude.dao.UserMetadataFieldDao {
  private final val log = LoggerFactory.getLogger(getClass)

  def addConstraintValue(fieldId: String, constraintValue: String)(implicit user: User, txId: TransactionId) = {
    log.info(s"Adding constraint value [$constraintValue] to field [$fieldId]")
    val query = MongoDBObject("_id" -> fieldId)

    val fieldOpt: Option[JsObject] = getById(fieldId)
    if (fieldOpt.isEmpty) throw NotFoundException(s"Cannot find user metadata field by ID [$fieldId]")

    val field: UserMetadataField = fieldOpt.get

    // if the constraint list is empty - create it
    if (field.constraintList.isEmpty) {
      log.debug(s"User [$user]. Constraint list for [${field.name}] empty - creating it.")
      COLLECTION.update(query, MongoDBObject("$set" ->
        MongoDBObject(C.MetadataField.CONSTRAINT_LIST -> List())))
    }

    val cmd = MongoDBObject("$addToSet" ->
      MongoDBObject(C.MetadataField.CONSTRAINT_LIST -> constraintValue))

    COLLECTION.update(query, cmd)
  }

  def deleteConstraintValue(fieldId: String, constraintValue: String)(implicit user: User, txId: TransactionId) = {
    val cmd = MongoDBObject("$pull" ->
      MongoDBObject(C.MetadataField.CONSTRAINT_LIST -> constraintValue))

    val query = MongoDBObject("_id" -> fieldId)
    COLLECTION.update(query, cmd)

    /* if this is the last value in the list -
       undefine the list so we are always clear about it being empty */
    val field: UserMetadataField = getById(fieldId).get

    if (field.constraintList.contains(List())) {
      log.debug(s"User [$user]. Undefining constraint list for [${field.name}].")
      COLLECTION.update(query, MongoDBObject("$unset" ->
        MongoDBObject(C.MetadataField.CONSTRAINT_LIST -> "")))
    }
  }
}
