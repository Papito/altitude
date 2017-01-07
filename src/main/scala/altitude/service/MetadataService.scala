package altitude.service

import altitude.dao.{AssetDao, MetadataFieldDao, NotImplementedDao}
import altitude.exceptions.{NotFoundException, DuplicateException, ValidationException}
import altitude.models.search.{Query, QueryResult}
import altitude.models.{Metadata, FieldType, MetadataField}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, JsString, JsObject}


class MetadataService(app: Altitude) extends BaseService[MetadataField](app){
  private final val log = LoggerFactory.getLogger(getClass)

  protected val METADATA_FIELD_DAO = app.injector.instance[MetadataFieldDao]
  protected val ASSET_DAO = app.injector.instance[AssetDao]

  // this is a combo service so it does not have its own DAO
  override protected val DAO = new NotImplementedDao(app)

  def addField(metadataField: MetadataField)
              (implicit ctx: Context, txId: TransactionId = new TransactionId): MetadataField = {

    txManager.withTransaction[MetadataField] {
      val existing = METADATA_FIELD_DAO.query(Query(Map(
        C.MetadataField.NAME_LC -> metadataField.nameLowercase
      )))

      if (existing.nonEmpty) {
        log.debug(s"Duplicate found for field [${metadataField.name}]")
        throw DuplicateException(metadataField, existing.records.head)
      }

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
      val _metadata = cleanAndValidateMetadata(metadata)
      ASSET_DAO.setMetadata(assetId = assetId, metadata = _metadata)
    }
  }

  def updateMetadata(assetId: String, metadata: Metadata)
                 (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.info(s"Updating metadata for asset [$assetId]: $metadata")

    txManager.withTransaction {
      val _metadata = cleanAndValidateMetadata(metadata)
      ASSET_DAO.updateMetadata(assetId, _metadata)
    }
  }

  /**
   * Makes sure the metadata fields are configured in this syste,m after common-sense data
   * hygiene
   *
   * @return clean, de-duplicated copy of the metadata
   */
  private def cleanAndValidateMetadata(metadata: Metadata)
                                      (implicit ctx: Context, txId: TransactionId ) = {
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
      val values: Set[String] = m._2
      // trim all values and discard blanks
      val trimmed = values.map(_.trim).filter(_.nonEmpty)
      res + (fieldId -> trimmed)
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
        ex.errors += (field.name ->
          C.Msg.Warn.DUPLICATE_FIELD_VALUE.format(field.name, values.mkString(", ")))
      }
    }

    if (ex.nonEmpty)
      throw ex

    new Metadata(cleanData)
  }
}
