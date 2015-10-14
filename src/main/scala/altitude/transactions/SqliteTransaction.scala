package altitude.transactions

import java.sql.Connection

class SqliteTransaction(conn: Connection) extends JdbcTransaction(conn) {

}
