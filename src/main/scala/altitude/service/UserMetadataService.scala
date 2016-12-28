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
        case _ =>
          val field: UserMetadataField = fieldOpt.get

          val ret = UserMetadataField(
            name = field.name,
            fieldType = field.fieldType,
            maxLength = field.maxLength)

          Some(ret)
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
}
