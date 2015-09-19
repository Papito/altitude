package altitude.dao.postgres

import org.slf4j.Logger


trait Postgres {
  val log: Logger
  val tableName: String
}
