package forex.services.rates.interpreters.oneframe

import cats.effect.{ContextShift, IO}
import forex.config.Config
import forex.domain.Rate
import org.http4s.Uri.Path.Segment
import org.http4s.Uri.Scheme.http
import org.http4s.Uri.{Authority, RegName}
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.ci.CIString

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

  def getOneFrameData(pair: Rate.Pair): Rate = {
    resource
      .use(client => client.expect[List[GetOneFrameResponse]](buildRequest(pair)))
      //bad op, need to fix
      .unsafeRunSync()
      .last
      .asRate
  }

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
      .withQueryParam("pair", pair)
}
