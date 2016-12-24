package altitude.dao

import altitude.Context
import altitude.transactions.TransactionId

trait UserMetadataFieldDao extends BaseDao {
  def addConstraintValue(fieldId: String, constraintValue: String)(implicit ctx: Context, txId: TransactionId)
  def deleteConstraintValue(fieldId: String, constraintValue: String)(implicit ctx: Context, txId: TransactionId)
}