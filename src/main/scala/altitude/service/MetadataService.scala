package altitude.service

import altitude.dao.{MetadataFieldDao, NotImplementedDao}
import altitude.exceptions.{NotFoundException, DuplicateException, ValidationException}
import altitude.models.{Metadata, FieldType, MetadataField}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, JsObject}


class MetadataService(app: Altitude) extends BaseService[MetadataField](app){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val METADATA_FIELD_DAO = app.injector.instance[MetadataFieldDao]
  override protected val DAO = new NotImplementedDao(app)

  def addField(metadataField: MetadataField)
              (implicit ctx: Context, txId: TransactionId = new TransactionId): MetadataField = {

    txManager.withTransaction[MetadataField] {
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
          val field: MetadataField = fieldOpt.get

          val ret = MetadataField(
            name = field.name,
            fieldType = field.fieldType)

          Some(ret)
      }
    }

  /**
   * Returns a lookup map (by ID) of all configured fields in this repository
   */
  def getAllFields(implicit ctx: Context, txId: TransactionId = new TransactionId): Map[String, MetadataField] =
    txManager.asReadOnly[Map[String, MetadataField]] {
      METADATA_FIELD_DAO.getAll.map{ res =>
        val metadataField: MetadataField = res
        metadataField.id.get -> metadataField
      }.toMap
    }

  def deleteFieldById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Int =
    txManager.withTransaction[Int] {
      METADATA_FIELD_DAO.deleteById(id)
    }

  def setMetadata(assetId: String, metadata: Metadata)
               (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    log.info(s"Setting metadata for asset [$assetId]: $metadata")

    txManager.withTransaction {
      val fields = getAllFields

      // make sure we have all metadata field IDs
      val existingFieldIds = fields.keys.toSet
      val suppliedFieldIds = metadata.keys

      suppliedFieldIds.diff(existingFieldIds) match {
        case missing: Set[String] if missing.nonEmpty =>
          throw NotFoundException(
            s"Fields [${missing.mkString(", ")}] are not supported by this repository"
          )
        case _ =>
      }
      /*
            if (fieldOpt.isEmpty) {
              throw NotFoundException(s"Cannot find user metadata field by ID [$fieldId]")
            }

            val field: MetadataField = fieldOpt.get
            log.info(s"Adding values to [${field.name}]: [${values.mkString(" ,")}}]")

            val trimmedValues = values
              .map(_.toLowerCase)
              .map(_.trim)
              .filter(_.nonEmpty)
            // check for duplicates
            val existingConstraintValues = field.constraintList.getOrElse(List[String]())

            if (existingConstraintValues.contains(trimmedValue)) {
              // duplicate exception expects model json for both this object and the duplicate
              val o = JsObject(Seq(C.MetadataConstraintValue.CONSTRAINT_VALUE -> JsString(constraintValueLc)))
              throw new DuplicateException(o, o)
            }
      */
    }
  }

}
