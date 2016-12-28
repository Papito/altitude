package altitude.dao.mongo

import altitude.exceptions.NotFoundException
import altitude.models.MetadataField
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject


class MetadataFieldDao(val app: Altitude) extends BaseMongoDao("metadata_fields")
  with altitude.dao.MetadataFieldDao