package software.altitude.core.dao.jdbc.querybuilder

/**
 * This is the result of SqlQueryBuilder
 * @param sqlAsString the SQL query to be used - with bind placeholders instead of values
 * @param bindValues array of bind values of the SQL statement returned
 */
case class SqlQuery(sqlAsString: String, bindValues: List[Any])
