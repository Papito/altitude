package altitude.dao.mongo

import altitude.models.{BaseModel, Folder}
import altitude.{Const => C, Altitude}
import altitude.transactions.TransactionId
import play.api.libs.json.{Json, JsObject}

class FolderDao(val app: Altitude) extends BaseMongoDao("folders") with altitude.dao.FolderDao {

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val folder = jsonIn: Folder

    val folderId = BaseModel.genId

    // parent id is THIS id, unless other parent id is given
    val parentId = folder.parentId match {
      case None => folderId
      case _ => folder.parentId.get
    }

    val json = jsonIn ++ Json.obj(
      C.Base.ID -> folderId, C.Folder.PARENT_ID -> parentId)

    super.add(json)
  }

}
