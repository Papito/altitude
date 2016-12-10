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
    // verify that the field type is allowed
    if (!FieldType.values.exists(v => v.toString == metadataField.fieldType.toUpperCase)) {
      val ex = ValidationException()
      ex.errors += (C("MetadataField.FIELD_TYPE") ->
        C("msg.err.wrong_value").format(FieldType.values.mkString(", ")))
      throw ex
    }

    METADATA_FIELD_DAO.add(metadataField)
  }

  def getFieldByName(name: String)
                    (implicit user: User, txId: TransactionId = new TransactionId): Option[UserMetadataField] = {
    val q = Query(user, params = Map(C("MetadataField.NAME_LC") -> name.toLowerCase))
    val res: QueryResult = METADATA_FIELD_DAO.query(q)

    if (res.records.length > 1)
      throw new Exception("getFieldByName should return only a single result")

    if (!res.nonEmpty) None else Some(res.records.head)
  }

  def getFieldById(id: String)(implicit user: User, txId: TransactionId = new TransactionId): Option[JsObject] =
      METADATA_FIELD_DAO.getById(id)

  def getAllFields()(implicit user: User, txId: TransactionId = new TransactionId): List[JsObject] =
      METADATA_FIELD_DAO.getAll


  def deleteFieldById(id: String)(implicit user: User, txId: TransactionId = new TransactionId): Int =
      METADATA_FIELD_DAO.deleteById(id)

  def addConstraintValue(fieldId: String, constraintValue: String)
                        (implicit user: User, txId: TransactionId = new TransactionId) = {

    txManager.withTransaction {
      // get the field we are working with
      val fieldOpt = getFieldById(fieldId)

      if (fieldOpt.isEmpty) {
        throw NotFoundException(s"Cannot find field by ID [$fieldId]")
      }

      val field: UserMetadataField = fieldOpt.get
      log.info(s"Adding constraint value [$constraintValue] to field ${field.name}")

      // TODO: make sure we don't already have it

      METADATA_FIELD_DAO.addConstraintValue(fieldId, constraintValue)
    }
  }
}
