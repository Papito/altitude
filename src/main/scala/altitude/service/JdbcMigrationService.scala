package altitude.service

import altitude.Altitude
import altitude.dao.MigrationDao
import altitude.transactions.{JdbcTransactionManager, TransactionId, AbstractTransactionManager}
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._

abstract class JdbcMigrationService(app: Altitude) extends MigrationService {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO: MigrationDao
  protected val txManager = app.injector.instance[AbstractTransactionManager]
  val FILE_EXTENSION = ".sql"

  log.info("JDBC migration service initialized")

  override def runMigration(version: Int): Unit = {
    val migrationCommands = parseMigrationCommands(version)
    val txManager = new JdbcTransactionManager(app)

    val conn = txManager.connection
    for (sql <- migrationCommands) {
      log.info(s"Executing $sql")
      val stmt = conn.createStatement()
      stmt.executeUpdate(sql)
      stmt.close()
    }

    conn.close()
  }

  override def existingVersion(implicit txId: TransactionId = new TransactionId): Int = {
    txManager.asReadOnly[Int] {
      DAO.currentVersion
    }
  }

}
