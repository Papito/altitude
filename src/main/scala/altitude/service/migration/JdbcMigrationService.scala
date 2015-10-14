package altitude.service.migration

import altitude.Altitude
import altitude.dao.MigrationDao
import altitude.transactions.{AbstractTransactionManager, JdbcTransactionManager, TransactionId}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

abstract class JdbcMigrationService(app: Altitude) extends MigrationService {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO: MigrationDao
  protected val txManager = app.injector.instance[JdbcTransactionManager]
  val FILE_EXTENSION = ".sql"

  log.info("JDBC migration service initialized")

  override def runMigration(version: Int): Unit = {
    val migrationCommands = parseMigrationCommands(version)

    val conn = txManager.connection
    for (sql <- migrationCommands) {
      log.info(s"Executing $sql")
      val stmt = conn.createStatement()
      stmt.executeUpdate(sql)
      stmt.close()
    }

    txManager.closeConnection(conn)
  }

  override def existingVersion(implicit txId: TransactionId = new TransactionId): Int = {
    txManager.asReadOnly[Int] {
      DAO.currentVersion
    }
  }

}
