package forex.services.rates.interpreters.oneframe

import cats.effect.{Async, BracketThrow, IO}
import forex.domain.Currency.{JPY, USD}
import forex.domain.Rate.Pair
import forex.domain.{Price, Rate, Timestamp}
import forex.programs.rates.errors.toProgramError
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.http4s.dsl.Http4sDsl
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.temporal.ChronoUnit
import java.time._

//add validation for token
class OneFrameClientTest extends AnyFunSuite with Http4sDsl[IO] with Matchers {
  implicit val bracket: BracketThrow[IO] = Async[IO]

  val oneFrameClient: OneFrameClient = new OneFrameClient

  test("Get good data from one frame") {
    OneFrameTestServer.response = Ok("""[
                                       |  {
                                       |    "from": "USD",
                                       |    "to": "JPY",
                                       |    "bid": 0.9519282811356304,
                                       |    "ask": 0.13460390086691998,
                                       |    "price": 0.54326609100127519,
                                       |    "time_stamp": "2024-10-15T12:56:50.063Z"
                                       |  },
                                       |  {
                                       |    "from": "JPY",
                                       |    "to": "USD",
                                       |    "bid": 0.6588966626132594,
                                       |    "ask": 0.4033633060536129,
                                       |    "price": 0.53112998433343615,
                                       |    "time_stamp": "2024-10-15T12:56:50.063Z"
                                       |  }
                                       |]""".stripMargin)
    OneFrameTestServer.expectedPairs = List("USDJPY", "JPYUSD")

    OneFrameTestServer.start
      .use { _ =>
        for {
          response <- oneFrameClient.loadPair(Pair(USD, JPY)).left.map(e => fail(toProgramError(e)))
        } yield {
          response.size shouldBe 2
          response.get(Pair(USD, JPY)) shouldBe Some(
            Rate(
              Pair(USD, JPY),
              Price(BigDecimal("0.54326609100127519")),
              Timestamp(
                OffsetDateTime
                  .of(
                    LocalDateTime.of(LocalDate.of(2024, 10, 15), LocalTime.of(12, 56, 50).plus(63, ChronoUnit.MILLIS)),
                    ZoneOffset.UTC
                  )
                  .toInstant
                  .atOffset(OffsetDateTime.now.getOffset)
              )
            )
          )
          response.get(Pair(JPY, USD)) shouldBe Some(
            Rate(
              Pair(JPY, USD),
              Price(BigDecimal("0.53112998433343615")),
              Timestamp(
                OffsetDateTime
                  .of(
                    LocalDateTime.of(LocalDate.of(2024, 10, 15), LocalTime.of(12, 56, 50).plus(63, ChronoUnit.MILLIS)),
                    ZoneOffset.UTC
                  )
                  .toInstant
                  .atOffset(OffsetDateTime.now.getOffset)
              )
            )
          )
        }

        IO.unit
      }
      .unsafeRunSync()
  }

  test("Get bad data from one frame") {
    OneFrameTestServer.response = Ok("""[
                                       |  {
                                       |    "from": "USD1",
                                       |    "to": "JPY1",
                                       |    "bid": 0.9519282811356304,
                                       |    "ask": 0.13460390086691998,
                                       |    "price": 0.54326609100127519,
                                       |    "time_stamp": "2024-10-15T12:56:50.063Z"
                                       |  },
                                       |  {
                                       |    "from": "JPY",
                                       |    "to": "USD",
                                       |    "bid": 0.6588966626132594,
                                       |    "ask": 0.4033633060536129,
                                       |    "price": some,
                                       |    "time_stamp": "2024-10-15T12:56:50.063Z"
                                       |  }
                                       |]""".stripMargin)
    OneFrameTestServer.expectedPairs = List("USDJPY", "JPYUSD")

    OneFrameTestServer.start
      .use { _ =>
        for {
          response <- oneFrameClient.loadPair(Pair(USD, JPY)).swap.left.map(r => fail(s"Unexpected rate received $r"))
        } yield {
          response shouldBe OneFrameLookupFailed("Unable to retrieve data for pair = Pair(USD,JPY), pls try again later")
        }
        IO.unit
      }
      .unsafeRunSync()
  }

  test("Test empty data from one frame") {
    OneFrameTestServer.response = Ok("[]")
    OneFrameTestServer.expectedPairs = List("USDJPY", "JPYUSD")

    OneFrameTestServer.start
      .use { _ =>
        for {
          response <- oneFrameClient.loadPair(Pair(USD, JPY)).swap.left.map(r => fail(s"Unexpected rate received $r"))
        } yield {
          response shouldBe OneFrameLookupFailed("Unable to process request for pair = Pair(USD,JPY), pls try again later")
        }
        IO.unit
      }
      .unsafeRunSync()
  }

  test("Bad pair with same") {
    OneFrameTestServer.response = Ok("[]")
    OneFrameTestServer.expectedPairs = List("USDUSD", "USDUSD")
    OneFrameTestServer.start
      .use { _ =>
        for {
          response <- oneFrameClient.loadPair(Pair(USD, USD)).swap.left.map(r => fail(s"Unexpected rate received $r"))
        } yield {
          response shouldBe OneFrameLookupFailed("Unable to process request for pair = Pair(USD,USD), pls try again later")
        }
        IO.unit
      }
      .unsafeRunSync()
  }

  test("Test OneFrame unavailable") {
    OneFrameTestServer.response = ServiceUnavailable()
    OneFrameTestServer.expectedPairs = List("USDJPY", "JPYUSD")
    OneFrameTestServer.start
      .use { _ =>
        for {
          response <- oneFrameClient.loadPair(Pair(USD, JPY)).swap.left.map(r => fail(s"Unexpected rate received $r"))
        } yield {
          response shouldBe OneFrameLookupFailed("Unable to retrieve data for pair = Pair(USD,JPY), pls try again later")
        }
        IO.unit
      }
      .unsafeRunSync()
  }

  test("Test OneFrame bad request") {
    OneFrameTestServer.response = BadRequest()
    OneFrameTestServer.expectedPairs = List("USDJPY", "JPYUSD")
    OneFrameTestServer.start
      .use { _ =>
        for {
          response <- oneFrameClient.loadPair(Pair(USD, JPY)).swap.left.map(r => fail(s"Unexpected rate received $r"))
        } yield {
          response shouldBe OneFrameLookupFailed("Unable to retrieve data for pair = Pair(USD,JPY), pls try again later")
        }
        IO.unit
      }
      .unsafeRunSync()
  }

}
