package altitude.service

import altitude.dao.{NotImplementedDao, UserMetadataFieldDao}
import altitude.exceptions.{NotFoundException, ValidationException}
import altitude.models.search.{Query, QueryResult}
import altitude.models.{FieldType, User, UserMetadataField}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject


class UserMetadataService(app: Altitude) extends BaseService[UserMetadataField](app){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val METADATA_FIELD_DAO = app.injector.instance[UserMetadataFieldDao]
  override protected val DAO = new NotImplementedDao(app)

  def addField(metadataField: UserMetadataField)
              (implicit user: User, txId: TransactionId = new TransactionId): UserMetadataField = {

    txManager.withTransaction[UserMetadataField] {
      // verify that the field type is allowed
      if (!FieldType.values.exists(v => v.toString == metadataField.fieldType.toUpperCase)) {
        val ex = ValidationException()
        ex.errors += (C("MetadataField.FIELD_TYPE") ->
          C("msg.err.wrong_value").format(FieldType.values.mkString(", ")))
        throw ex
      }

    METADATA_FIELD_DAO.add(metadataField)
    }
  }

  def getFieldById(id: String)(implicit user: User, txId: TransactionId = new TransactionId): Option[JsObject] =
    txManager.asReadOnly[Option[JsObject]] {
      val fieldOpt = METADATA_FIELD_DAO.getById(id)

      fieldOpt match {
        case None => None
        case _ => {
          val field: UserMetadataField = fieldOpt.get

          val sortedConstraintList = field.constraintList match {
            case None => None
            case _ => Some(field.constraintList.get.sorted)
          }

          val ret = UserMetadataField(
            userId = user.id.get,
            name = field.name,
            fieldType = field.fieldType,
            maxLength = field.maxLength,
            constraintList = sortedConstraintList)

          Some(ret)
        }
      }
    }

  def getAllFields()(implicit user: User, txId: TransactionId = new TransactionId): List[JsObject] =
    txManager.asReadOnly[List[JsObject]] {
      METADATA_FIELD_DAO.getAll
    }

  def deleteFieldById(id: String)(implicit user: User, txId: TransactionId = new TransactionId): Int =
    txManager.withTransaction[Int] {
      METADATA_FIELD_DAO.deleteById(id)
    }

  def addConstraintValue(fieldId: String, constraintValue: String)
                        (implicit user: User, txId: TransactionId = new TransactionId) = {

    txManager.withTransaction {
      // get the field we are working with
      val fieldOpt = getFieldById(fieldId)

      if (fieldOpt.isEmpty) {
        throw NotFoundException(s"Cannot find user metadata field by ID [$fieldId]")
      }

      val field: UserMetadataField = fieldOpt.get
      log.info(s"Adding constraint value [$constraintValue] to field ${field.name}")

      /* TODO:

          * clean (remove double spaces, tabs, and line breaks)
          * validate
          * make sure we don't already have the constraint
          * lowercase
       */

      METADATA_FIELD_DAO.addConstraintValue(fieldId, constraintValue)
    }
  }

  def deleteConstraintValue(fieldId: String, constraintValue: String)
                           (implicit user: User, txId: TransactionId = new TransactionId) = {
    log.info(s"User: $user. Deleting constraint value [$constraintValue] for field [$fieldId]")

    txManager.withTransaction {
      METADATA_FIELD_DAO.deleteConstraintValue(fieldId, constraintValue)
    }
  }
}
