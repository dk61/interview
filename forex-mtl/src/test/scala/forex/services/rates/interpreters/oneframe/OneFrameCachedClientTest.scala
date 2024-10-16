package forex.services.rates.interpreters.oneframe

import forex.domain.Currency.{JPY, USD}
import forex.domain.Rate.Pair
import forex.domain.RateOps.PairOps
import forex.domain.{Price, Rate, Timestamp}
import forex.programs.rates.errors.toProgramError
import forex.services.rates.errors.Error.{BadPairProvided, OneFrameLookupFailed}
import org.mockito.MockitoSugar.{mock, times, verify, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
//need to fix when same currency for from and to USD -> USD
class OneFrameCachedClientTest extends AnyFunSuite with Matchers {
  val pair: Pair        = Pair(USD, JPY)
  val swappedPair: Pair = pair.swap

  test("Good data & no repeatable attempts & Swap check") {
    val expectedTs = Timestamp.now
    val clientMock = mock[OneFrameClient]

    when(clientMock.loadPair(pair))
      .thenReturn(
        Right(
          Map(
            pair -> Rate(pair, Price(0.5), expectedTs),
            swappedPair -> Rate(swappedPair, Price(0.5), expectedTs)
          )
        )
      )

    val cachedClient: OneFrameCachedClient = new OneFrameCachedClient(clientMock)

    for {
      directResponse <- cachedClient.getOneFrameData(pair).left.map(e => fail(toProgramError(e)))
      oppositeResponse <- cachedClient.getOneFrameData(swappedPair).left.map(e => fail(toProgramError(e)))
    } yield {
      directResponse shouldBe Rate(pair, Price(0.5), expectedTs)
      oppositeResponse shouldBe Rate(swappedPair, Price(0.5), expectedTs)
    }

    for {
      _ <- 1 to 100
    } {
      cachedClient.getOneFrameData(pair)
      cachedClient.getOneFrameData(swappedPair)
    }

    verify(clientMock, times(1)).loadPair(pair)
    verify(clientMock, times(0)).loadPair(swappedPair)
  }

  test("Bad data & and repeatable attempts & Swap check") {
    val clientMock = mock[OneFrameClient]

    when(clientMock.loadPair(pair))
      .thenReturn(
        Left(
          OneFrameLookupFailed(s"Unable to retrieve data for pair = $pair, pls try again later")
        )
      )

    when(clientMock.loadPair(swappedPair))
      .thenReturn(
        Left(
          OneFrameLookupFailed(s"Unable to retrieve data for pair = $swappedPair, pls try again later")
        )
      )

    val cachedClient: OneFrameCachedClient = new OneFrameCachedClient(clientMock)

    for {
      directResponse <- cachedClient.getOneFrameData(pair).swap.left.map(r => fail(s"Unexpected rate received $r"))
      oppositeResponse <- cachedClient.getOneFrameData(swappedPair).swap.left.map(r => fail(s"Unexpected rate received $r"))
    } yield {
      directResponse shouldBe OneFrameLookupFailed(s"Unable to retrieve data for pair = $pair, pls try again later")
      oppositeResponse shouldBe OneFrameLookupFailed(
        s"Unable to retrieve data for pair = $swappedPair, pls try again later"
      )
    }

    for {
      _ <- 1 to 100
    } {
      cachedClient.getOneFrameData(pair)
      cachedClient.getOneFrameData(swappedPair)
    }

    verify(clientMock, times(101)).loadPair(pair)
    verify(clientMock, times(101)).loadPair(swappedPair)
  }

  test("Test invalid pair provided") {
    val clientMock                         = mock[OneFrameClient]
    val cachedClient: OneFrameCachedClient = new OneFrameCachedClient(clientMock)
    val badPair: Pair                      = Pair(USD, USD)

    for {
      directResponse <- cachedClient.getOneFrameData(badPair).swap.left.map(r => fail(s"Unexpected rate received $r"))
    } yield {
      directResponse shouldBe BadPairProvided(s"Pair always should be different, has been provided $badPair")
    }
    verify(clientMock, times(0)).loadPair(badPair)
  }
}
