package altitude.util

object Query {
  object ParamType extends Enumeration {
    val EQ, GT, LT, GTE, LTE, IN, RANGE, OR, CONTAINS, MATCHES = Value
  }

  case class QueryParam private (values: Set[Any], paramType: ParamType.Value, negate: Boolean = false) {
    require(values.nonEmpty)

    // a range requires two values
    if (paramType == ParamType.RANGE || paramType == ParamType.OR)
      require(values.size == 2)

    // overloaded to accept one value
    def this (value: Any, paramType: ParamType.Value, negate: Boolean) =
      this(Set(value), paramType, negate)

    // overloaded to accept two values
    def this(value1: Any, value2: Any, paramType: ParamType.Value, negate: Boolean) =
      this(value1 :: value2 :: Nil, paramType, negate)

    // overloaded as simple equals
    def this(value: Any) =
      this(value, ParamType.EQ, negate = false)
  }

  def EQUALS(value: Any) = new QueryParam(value)
  def NOT_EQUALS(value: Any) = new QueryParam(value, ParamType.EQ, negate = true)

  def LT(value: Any) = new QueryParam(value, ParamType.LT)
  def NOT_LT(value: Any) = new QueryParam(value, ParamType.LT, negate = true)

  def LTE(value: Any) = new QueryParam(value, ParamType.LTE)
  def NOT_LTE(value: Any) = new QueryParam(value, ParamType.LTE, negate = true)

  def GT(value: Any) = new QueryParam(value, ParamType.GT)
  def NOT_GT(value: Any) = new QueryParam(value, ParamType.GT, negate = true)

  def GTE(value: Any) = new QueryParam(value, ParamType.GTE)
  def NOT_GTE(value: Any) = new QueryParam(value, ParamType.GTE, negate = true)

  def RANGE(values: List[Any]) = new QueryParam(values, ParamType.RANGE)
  def NOT_IN_RANGE(values: List[Any]) = new QueryParam(values, ParamType.RANGE, negate = true)

  def OR(values: List[Any]) = new QueryParam(values, ParamType.OR)
  def NOT_OR(values: List[Any]) = new QueryParam(values, ParamType.OR, negate = true)

  def IN(values: List[Any], negate: Boolean = false) = {
    // if only one value given - simplify this to be just an equals
    if (values.size == 1) {
      new QueryParam(values.head, negate)
    } else {
      new QueryParam(values, ParamType.IN, negate)
    }
  }
  def NOT_IN(values: List[Any]) = IN(values, negate = true)

  def CONTAINS (value: Any) = new QueryParam(value, ParamType.CONTAINS)
  def NOT_CONTAINS(value: Any) = new QueryParam(value, ParamType.CONTAINS, negate = true)

  def MATCHES(value: Any) = new QueryParam(value, ParamType.MATCHES)
  def NOT_MATCHES(value: Any) = new QueryParam(value, ParamType.MATCHES, negate = true)
}

case class Query(params: Map[String, Any] = Map(), rpp: Int = 0, page: Int = 1) {
  if (rpp < 0) throw new IllegalArgumentException(s"Invalid results per page value: $rpp")
  if (page < 1) throw new IllegalArgumentException(s"Invalid page value: $page")
}
