package altitude.dao.jdbc

/**
 * This is the result of SqlQueryBuilder
 * @param queryString the SQL query to be used - with bind placeholders instead of values
 * @param selectBindValues array of bind values of the SQL statement returned
 */
case class SqlQuery(queryString: String, selectBindValues: List[Object])