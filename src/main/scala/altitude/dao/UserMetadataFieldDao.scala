package altitude.dao

import altitude.models.User
import altitude.transactions.TransactionId

trait UserMetadataFieldDao extends BaseDao {
  def addConstraintValue(fieldId: String, constraintValue: String)(implicit user: User, txId: TransactionId)
}