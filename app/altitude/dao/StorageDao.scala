/*
    _._ _..._ .-',     _.._(`))
   '-. `     '  /-._.-'    ',/
      )         \            '.
     / _    _    |             \
    |  a    a    /              |
    \   .-.                     ;
     '-('' ).-'       ,'       ;
        '-;           |      .'
           \           \    /
           | 7  .__  _.-\   \
           | |  |  ``/  /`  /
      jgs /,_|  |   /,_/   /
             /,_/      '`-'

 */
package altitude.dao

import altitude.models.{StorageType, Storage}
import play.api.libs.json.JsObject
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future


class StorageDao extends BaseDao {
  override def add(json: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    Future {json}
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[Option[JsObject]] = {
    val fsStorage = Storage(id = Some("1"), name="local", storageType = StorageType.file_system)
    Future {Some(fsStorage)}
  }

  override def getAll()(implicit txId: TransactionId): Future[List[JsObject]] = {
    val fsStorage = Storage(id = Some("1"), name="local", storageType = StorageType.file_system)
    Future{ List(fsStorage) }
  }
}
