package altitude.dao.mongo

import altitude.Altitude
import altitude.models.User
import altitude.transactions.TransactionId

class UserMetadataFieldDao(val app: Altitude) extends BaseMongoDao("metadata_fields") with altitude.dao.UserMetadataFieldDao {
  def addConstraintValue(fieldId: String, constraintValue: String)(implicit user: User, txId: TransactionId) = {
  }
}
