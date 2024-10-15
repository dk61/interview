package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.domain.Rate
import forex.programs.RatesProgram
import forex.programs.rates.errors.ClientError.BadInputParameters
import forex.programs.rates.errors.ProgramError
import forex.programs.rates.errors.ServerError.RateLookupFailed
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, Response }

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates
        .get(RatesProgramProtocol.GetRatesRequest(from, to))
        .flatMap(mapResult)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

  private def mapResult(result: Either[ProgramError, Rate]): F[Response[F]] =
    result match {
      case Right(r) => prepareResponse(r)
      case Left(r)  => parseError(r)
    }

  private def prepareResponse(rate: Rate): F[Response[F]] = {
    Ok(rate.asGetApiResponse)
  }

  private def parseError(err: ProgramError): F[Response[F]] =
    err match {
      case BadInputParameters(msg) => BadRequest(msg)
      case RateLookupFailed(msg)   => ServiceUnavailable(msg)
      case _                       => InternalServerError(err.asErrorResponse)
    }

}
