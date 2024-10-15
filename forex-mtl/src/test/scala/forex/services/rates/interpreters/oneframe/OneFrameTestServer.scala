package forex.services.rates.interpreters.oneframe

import cats.data.Validated
import cats.effect.{ContextShift, IO, Resource, Timer}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Server
import org.http4s.{HttpApp, HttpRoutes, Response}

import scala.concurrent.ExecutionContext.global

object OneFrameTestServer extends Http4sDsl[IO] {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  var response: IO[Response[IO]]  = Ok()
  var expectedPairs: List[String] = List()

  object PairQueryParam extends OptionalMultiQueryParamDecoderMatcher[String]("pair")

  def testApp: HttpApp[IO] = testRoutes.orNotFound

  def testRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "rates" :? PairQueryParam(Validated.Valid(pairs)) =>
      assert(expectedPairs == pairs)
      response
  }


  def start: Resource[IO, Server] = {
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(testApp)
      .resource
  }
}
