package altitude.dao

import altitude.models.{StorageType, Storage}
import play.api.libs.json.JsValue
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class StorageDao extends BaseDao {
  override def add(json: JsValue)(implicit txId: TransactionId): Future[JsValue] = {
    Future {json}
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[JsValue] = {
    val fsStorage = Storage(id = "1", name="local", storageType = StorageType.file_system)
    Future {fsStorage.toJson}
  }

  override def getAll()(implicit txId: TransactionId): Future[List[JsValue]] = {
    val fsStorage = Storage(id = "1", name="local", storageType = StorageType.file_system)
    Future{ List(fsStorage.toJson) }
  }
}
