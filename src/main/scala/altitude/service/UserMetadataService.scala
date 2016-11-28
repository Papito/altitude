package altitude.service

import altitude.dao.{FolderDao, UserMetadataFieldDao}
import altitude.models.search.{QueryResult, Query}
import altitude.models.{User, MetadataField, Folder}
import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory
import altitude.{Altitude, Cleaners, Const => C}
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsObject


class UserMetadataService(app: Altitude) extends BaseService[MetadataField](app){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val METADATA_FIELD_DAO = app.injector.instance[UserMetadataFieldDao]
  override protected val DAO = null

  def addField(metadataField: MetadataField)
              (implicit user: User, txId: TransactionId = new TransactionId): MetadataField = {
    METADATA_FIELD_DAO.add(metadataField)
  }

  def getFieldByName(name: String)
                    (implicit user: User, txId: TransactionId = new TransactionId): Option[MetadataField] = {
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
}
