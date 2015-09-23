package altitude.service

import altitude.Altitude
import altitude.dao.MigrationDao
import altitude.transactions.{TransactionId, AbstractTransactionManager}
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._

abstract class JdbcMigrationService(app: Altitude) extends MigrationService {
  private val log =  LoggerFactory.getLogger(getClass)
  protected val DAO: MigrationDao
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  val FILE_EXTENSION = ".sql"

  log.info("JDBC migration service initialized")

  override def runMigration(version: Int): Unit = {
    log.info(s"RUNNING MIGRATION TO VERSION $version")
    val evolutionFilePath = getClass.getResource(
      s"$ROOT_EVOLUTIONS_PATH$EVOLUTIONS_DIR$version$FILE_EXTENSION").getPath
    log.info(s"Evolution file path: $evolutionFilePath")
  }

  override def existingVersion(implicit txId: TransactionId = new TransactionId): Int = {
    txManager.asReadOnly[Int] {
      DAO.currentVersion
    }
  }

}
