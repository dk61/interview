package forex.services.rates.interpreters.oneframe

import forex.domain.Currency
import forex.domain.Rate.Pair
import org.http4s.QueryParamEncoder

object OneFrameQueryParams {

  private[oneframe] implicit val pairQueryParam: QueryParamEncoder[Pair] = QueryParamEncoder[String].contramap {
    pair => List(pair.from, pair.to).map(Currency.show.show).mkString
  }
}
