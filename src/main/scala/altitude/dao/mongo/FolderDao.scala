package altitude.dao.mongo

import altitude.models.{BaseModel, Folder}
import altitude.{Const => C, Altitude}
import altitude.transactions.TransactionId
import play.api.libs.json.{Json, JsObject}

class FolderDao(val app: Altitude) extends BaseMongoDao("folders") with altitude.dao.FolderDao