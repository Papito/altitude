package altitude.service

import altitude.dao.{FolderDao, UserMetadataFieldDao}
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
}
