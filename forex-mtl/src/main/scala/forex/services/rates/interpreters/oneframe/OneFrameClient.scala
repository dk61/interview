package forex.services.rates.interpreters.oneframe

import cats.effect.{ContextShift, IO}
import forex.config.Config
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.domain.RateOps.PairOps
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.http4s.Uri.Path.Segment
import org.http4s.Uri.Scheme.http
import org.http4s.Uri.{Authority, RegName}
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.ci.CIString
import forex.services.rates.errors.Error

import scala.concurrent.ExecutionContext.global

class OneFrameClient {
  import OneFrameConverters._
  import OneFrameProtocol._
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  private val resource = BlazeClientBuilder[IO](global).resource

  private val config = Config.read("app")

  private val token = config.currencyProvider.token
  private val port  = config.currencyProvider.port
  private val host  = config.currencyProvider.host

  /**
    * Loads pair for provided pair.
    * If has been requested USD->JPY automatically will request JPY->USD.
    * @param pair pair to request
    * @return Guaranteed map with requested Pair for both directions
    */
  def loadPair(pair: Pair): Either[Error, Map[Pair, Rate]] = {
    resource
      .use(client => client.expect[List[GetOneFrameResponse]](buildRequest(pair)))
      .option
      //bad op, need to fix
      .unsafeRunSync() match {
      case Some(l) => convertResponse(l, pair)
      case None => Left(OneFrameLookupFailed(s"Unable to retrieve data for pair = $pair, pls try again later"))
    }
  }

  private def convertResponse(response: List[GetOneFrameResponse], pair: Pair): Either[Error, Map[Pair, Rate]] = {
    val result = response.map(r => r.asRate)
      .map(r => (r.pair, r))
      .toMap[Pair, Rate]
    result match {
      case _ if isValidResponse(result.keySet, pair) => Right(result)
      case _ => Left(OneFrameLookupFailed(s"Unable to process request for pair = $pair, pls try again later"))
    }
  }

  /**
   * Validating retrieved data from OneFrame.
   * Condition: Pairs for both directions exists + pairs is 2
   * @param pairs pairs
   * @param pair pair to check
   * @return true if valid pairs has been returned
   */
  private def isValidResponse(pairs: Set[Pair], pair: Pair): Boolean =
    if (pairs.contains(pair) && pairs.contains(pair.swap) && pairs.size == 2) true else false

  private def buildRequest(pair: Rate.Pair): Request[IO] =
    Request[IO](
      method = Method.GET,
      uri = buildUri(pair),
      headers = Headers(headers = Header.Raw(CIString("token"), token))
    )

  private def buildUri(pair: Rate.Pair): Uri =
    Uri(
      scheme = Some(http),
      authority = Some(
        value = Authority(
          userInfo = None,
          host = RegName(host),
          port = Some(port)
        )
      )
    ).withPath(Uri.Path(segments = Vector(Segment("rates"))))
      .withMultiValueQueryParams(Map("pair" -> Seq(pair, pair.swap)))
}
