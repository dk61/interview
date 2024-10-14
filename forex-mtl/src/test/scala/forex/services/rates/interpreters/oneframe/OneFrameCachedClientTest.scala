package forex.services.rates.interpreters.oneframe

import forex.domain.Currency.{JPY, USD}
import forex.domain.Rate.Pair
import forex.domain.RateOps.PairOps
import forex.domain.{Price, Rate, Timestamp}
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.mockito.MockitoSugar.{mock, times, verify, when}
import org.scalatest.funsuite.AnyFunSuite
//need to fix when same currency for from and to USD -> USD
class OneFrameCachedClientTest extends AnyFunSuite {
  val pair: Pair = Pair(USD, JPY)
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
    val directResponse                     = cachedClient.getOneFrameData(pair)

    assert(directResponse.isRight)
    val directRate = directResponse.toOption.get

    assert(USD == directRate.pair.from)
    assert(JPY == directRate.pair.to)
    assert(Price(0.5) == directRate.price)
    assert(expectedTs == directRate.timestamp)

    val oppositeResponse = cachedClient.getOneFrameData(pair)
    assert(oppositeResponse.isRight)
    val oppositeRate = oppositeResponse.toOption.get

    assert(USD == oppositeRate.pair.from)
    assert(JPY == oppositeRate.pair.to)
    assert(Price(0.5) == oppositeRate.price)
    assert(expectedTs == oppositeRate.timestamp)

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
    val directResponse                     = cachedClient.getOneFrameData(pair)

    assert(directResponse.isLeft)
    val directError = directResponse.left.toOption.get

    directError match {
      case OneFrameLookupFailed(msg) => assert(s"Unable to retrieve data for pair = $pair, pls try again later" == msg)
    }

    val oppositeResponse = cachedClient.getOneFrameData(swappedPair)
    assert(oppositeResponse.isLeft)
    val oppositeError = oppositeResponse.left.toOption.get

    oppositeError match {
      case OneFrameLookupFailed(msg) => assert(s"Unable to retrieve data for pair = $swappedPair, pls try again later" == msg)
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
}
