package altitude.service

import altitude.Validators.ModelDataValidator
import altitude.dao.{AssetDao, MetadataFieldDao}
import altitude.models.{Asset, FieldType, Metadata, MetadataField}
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import altitude.util.Query
import altitude.{Const => C, _}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, JsArray, Json, JsObject}

import scala.util.control.Breaks._

object MetadataService {
  class MetadataFieldValidator
    extends ModelDataValidator(
      required = Some(
        List(C.MetadataField.NAME, C.MetadataField.FIELD_TYPE)))
}

class MetadataService(val app: Altitude) extends ModelValidation {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val txManager = app.injector.instance[AbstractTransactionManager]
  protected val METADATA_FIELD_DAO = app.injector.instance[MetadataFieldDao]
  protected val ASSET_DAO = app.injector.instance[AssetDao]
  private final val VALID_BOOLEAN_VALUES = Set("0", "1", "true", "false")

  final val METADATA_FIELD_VALIDATOR = new MetadataService.MetadataFieldValidator
  val METADATA_FIELD_CLEANER = Cleaners.Cleaner(
    trim = Some(List(C.MetadataField.NAME, C.MetadataField.FIELD_TYPE)))

  def addField(metadataField: MetadataField)
              (implicit ctx: Context, txId: TransactionId = new TransactionId): MetadataField = {

    txManager.withTransaction[MetadataField] {
      val cleaned: MetadataField = cleanAndValidate(
        metadataField, Some(METADATA_FIELD_CLEANER), Some(METADATA_FIELD_VALIDATOR))

      val existing = METADATA_FIELD_DAO.query(Query(Map(
        C.MetadataField.NAME_LC -> cleaned.nameLowercase
      )))

      if (existing.nonEmpty) {
        log.debug(s"Duplicate found for field [${cleaned.name}]")
        throw DuplicateException(cleaned, existing.records.head)
      }

      METADATA_FIELD_DAO.add(cleaned)
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

  def getMetadata(assetId: String)
                 (implicit ctx: Context, txId: TransactionId = new TransactionId): Metadata =
    // return the metadata or a new empty one if blank
    ASSET_DAO.getMetadata(assetId) match {
      case Some(metadata) => metadata
      case None => new Metadata()
    }

  def setMetadata(assetId: String, metadata: Metadata)
               (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.info(s"Setting metadata for asset [$assetId]: $metadata")

    txManager.withTransaction {
      val cleanMetadata = cleanAndValidateMetadata(metadata)
      ASSET_DAO.setMetadata(assetId = assetId, metadata = cleanMetadata)
    }
  }

  def updateMetadata(assetId: String, metadata: Metadata)
                 (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.info(s"Updating metadata for asset [$assetId]: $metadata")

    txManager.withTransaction {
      val cleanMetadata = cleanAndValidateMetadata(metadata)

      /**
       * If the cleaned metadata does not have fields found in the original -
       * those are empty and should be deleted
       */
      val deletedFields = metadata.data.keys.filterNot(cleanMetadata.data.keys.toSet.contains).toSet
      ASSET_DAO.updateMetadata(assetId, cleanMetadata, deletedFields)
    }
  }

  /**
   * Makes sure the metadata fields are configured in this system after common-sense data
   * hygiene. Validates correct type for anything
   *
   * @return clean, de-duplicated copy of the metadata
   */
  def cleanAndValidateMetadata(metadata: Metadata)
                              (implicit ctx: Context, txId: TransactionId): Metadata = {
    if (metadata.data.isEmpty) {
      return metadata
    }

    // get all metadata fields configured for this repository
    val fields = getAllFields

    // make sure all metadata field IDs given to us are known
    val existingFieldIds = fields.keys.toSet
    val suppliedFieldIds = metadata.keys

    val missing = suppliedFieldIds.diff(existingFieldIds)

    if (missing.nonEmpty)
      throw NotFoundException(
        s"Fields [${missing.mkString(", ")}] are not supported by this repository"
      )

    /**
     * Clean the metadata to be ready for validation
     */
    val cleanData = metadata.data.foldLeft(Map[String, Set[String]]()) { (res, m) =>
      val fieldId = m._1
      val field: MetadataField = fields(fieldId)
      val values: Set[String] = m._2

      val trimmed = field.fieldType match {
        case FieldType.KEYWORD => values
          // trim leading/trailing
          .map(_.trim)
          // compact multiple space characters into one
          .map(_.replaceAll("[\\s]{2,}", " ")) // match two or more spaces and make it one
          // force a space character to be vanilla whitespace
          .map(_.replaceAll("\\s", " "))
          // and lose the blanks
          .filter(_.nonEmpty)

        case FieldType.NUMBER | FieldType.BOOL => values
          // trim leading/trailing
          .map(_.trim)
          // and lose the blanks
          .filter(_.nonEmpty)

        case FieldType.TEXT => values.map(_.trim)
      }

      if (trimmed.nonEmpty)
        res + (fieldId -> trimmed)
      else
        res
    }

    /**
     * Validate for duplicates in passed data
     */
    val ex = ValidationException()

    cleanData.foreach { m =>
      val fieldId = m._1
      val values: Set[String] = m._2
      val valuesLower = values.map(_.toLowerCase)

      if (values.size != valuesLower.size) {
        val field: MetadataField = fields(fieldId)
        ex.errors += (field.id.get ->
          C.Msg.Warn.DUPLICATE_FIELD_VALUE.format(values.mkString(", ")))
      }
    }

    if (ex.nonEmpty)
      throw ex

    /**
     * Validate based on field type
     */

    // for each field
    cleanData.foreach { m =>
      val fieldId = m._1
      val field: MetadataField = fields(fieldId)
      val values: Set[String] = m._2

      breakable {
        // booleans cannot have multiple values
        if (field.fieldType == FieldType.BOOL && values.size > 1) {
          ex.errors += (field.id.get -> C.Msg.Warn.INCORRECT_VALUE_TYPE)
          break()
        }

        // gather illegal values
        val illegalValues: Set[Option[String]] = values.map { value =>
          field.fieldType match {
            case FieldType.NUMBER => try {
              value.toDouble
              None
            } catch {
              case _: Throwable => Some(value)
            }
            case FieldType.KEYWORD => None
            case FieldType.TEXT => None
            case FieldType.BOOL =>
              if (VALID_BOOLEAN_VALUES.contains(value.toLowerCase)) {
                None
              }
              else {
                Some(value)
              }
          }
          // get rid of None's - those are good
        }.filter(_.isDefined)

        // add to the validation exception if any
        if (illegalValues.nonEmpty) {
          ex.errors += (field.id.get ->
            C.Msg.Warn.INCORRECT_VALUE_TYPE.format(field.name, illegalValues.mkString(", ")))
        }
      }
    }

    if (ex.nonEmpty)
      throw ex

    new Metadata(cleanData)
  }

  /*
    Presentation-level JSON transformer for metadata. This augments the limiting metadata JSON object to supply
    the names of fields, pulled from field configuration that the Metadata domain object is not aware of.

    On the way in we get:
      field id -> values
      field id -> values

    We get out:
      ID -> field id  NAME -> field name  VALUES -> values
      ID -> field id  NAME -> field name  VALUES -> values TYPE -> type
   */
  def toJson(metadata: Metadata, allMetadataFields: Option[Map[String, MetadataField]] = None)
  (implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {

      val allFields = allMetadataFields.isDefined match {
        case true => allMetadataFields.get
        case false => getAllFields
      }

      metadata.data.foldLeft(Json.obj()) { (res, m) =>
        val fieldId = m._1
        val field: Option[MetadataField] = allFields.get(fieldId)

        res ++ Json.obj(
          C.MetadataField.ID -> fieldId,
          C.MetadataField.NAME -> field.get.name,
          C.MetadataField.FIELD_TYPE -> field.get.fieldType.toString,
          C.MetadataField.VALUES -> JsArray(m._2.toSeq.map(JsString))
        )
      }
    }
  }
}
