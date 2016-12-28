package altitude.service

import altitude.dao.{NotImplementedDao, UserMetadataFieldDao}
import altitude.exceptions.{DuplicateException, NotFoundException, ValidationException}
import altitude.models.{FieldType, UserMetadataField}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, JsObject}


class UserMetadataService(app: Altitude) extends BaseService[UserMetadataField](app){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val METADATA_FIELD_DAO = app.injector.instance[UserMetadataFieldDao]
  override protected val DAO = new NotImplementedDao(app)

  def addField(metadataField: UserMetadataField)
              (implicit ctx: Context, txId: TransactionId = new TransactionId): UserMetadataField = {

    txManager.withTransaction[UserMetadataField] {
      // verify that the field type is allowed
      if (!FieldType.values.exists(v => v.toString == metadataField.fieldType.toUpperCase)) {
        val ex = ValidationException()
        ex.errors += (C.MetadataField.FIELD_TYPE ->
          C.Msg.Err.WRONG_VALUE.format(FieldType.values.mkString(", ")))
        throw ex
      }

    // verify here lowercase name is unique before we hit the DB constraint (DuplicateException)

    METADATA_FIELD_DAO.add(metadataField)
    }
  }

  def getFieldById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Option[JsObject] =
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
            name = field.name,
            fieldType = field.fieldType,
            maxLength = field.maxLength,
            constraintList = sortedConstraintList)

          Some(ret)
        }
      }
    }

  def getAllFields()(implicit ctx: Context, txId: TransactionId = new TransactionId): List[JsObject] =
    txManager.asReadOnly[List[JsObject]] {
      METADATA_FIELD_DAO.getAll
    }

  def deleteFieldById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Int =
    txManager.withTransaction[Int] {
      METADATA_FIELD_DAO.deleteById(id)
    }

  def addConstraintValue(fieldId: String, constraintValue: String)
                        (implicit ctx: Context, txId: TransactionId = new TransactionId) = {

    txManager.withTransaction {
      // get the field we are working with
      val fieldOpt = getFieldById(fieldId)

      if (fieldOpt.isEmpty) {
        throw NotFoundException(s"Cannot find user metadata field by ID [$fieldId]")
      }

      val field: UserMetadataField = fieldOpt.get
      log.info(s"Adding constraint value [$constraintValue] to field ${field.name}")

      val constraintValueLc = constraintValue.trim.toLowerCase

      // trim spaces and replace any two or more adjacent spaces with one
      val trimmedValue = constraintValueLc.trim.replaceAll("[\\s]{2,}", " ")

      if (trimmedValue.isEmpty) {
        val ex = ValidationException()
        ex.errors += (C.MetadataConstraintValue.CONSTRAINT_VALUE -> C.Msg.Err.CANNOT_BE_EMPTY)
        throw ex
      }

      // check that the value respects max length setting, if the setting is defined
      field.maxLength match {
        case Some(maxLen) if trimmedValue.length > maxLen =>
          val ex = ValidationException()
          val msg = C.Msg.Err.VALUE_TOO_LONG.format(maxLen)
          ex.errors += (C.MetadataConstraintValue.CONSTRAINT_VALUE -> msg)
          throw ex
        case _ =>
      }

      // check for duplicates
      val existingConstraintValues = field.constraintList.getOrElse(List[String]())

      if (existingConstraintValues.contains(trimmedValue)) {
        // duplicate exception expects model json for both this object and the duplicate
        val o = JsObject(Seq(C.MetadataConstraintValue.CONSTRAINT_VALUE -> JsString(constraintValueLc)))
        throw new DuplicateException(o, o)
      }

      METADATA_FIELD_DAO.addConstraintValue(fieldId, trimmedValue)
    }
  }

  def deleteConstraintValue(fieldId: String, constraintValue: String)
                           (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.info(s"Deleting constraint value [$constraintValue] for field [$fieldId]")

    txManager.withTransaction {
      METADATA_FIELD_DAO.deleteConstraintValue(fieldId, constraintValue)
    }
  }
}
