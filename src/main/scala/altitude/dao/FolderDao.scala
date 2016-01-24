package altitude.dao

import altitude.models.Folder
import altitude.transactions.TransactionId

trait FolderDao extends BaseDao {
  def getSystemFolders(implicit txId: TransactionId): Map[String, Folder]
}
