package forex.services.rates.interpreters.oneframe
import com.github.blemale.scaffeine.{LoadingCache, Scaffeine}
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.domain.RateOps.PairOps
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.{BadPairProvided, OneFrameLookupFailed}

import scala.concurrent.duration.DurationInt

//Not sure does that initialization is good
class OneFrameCachedClient(val oneFrameClient: OneFrameClient) {

  private val cache: LoadingCache[Pair, Either[Error, Map[Pair, Rate]]] =
    Scaffeine()
      .expireAfter[Pair, Either[Error, Map[Pair, Rate]]](
        //Better to add circuit breaker to correctly calculate trouble on upstream side to not spam it
        create = (_, value) => if (value.isLeft) 0.seconds else 4.minutes + 59.seconds,
        update = (_, _, duration) => duration,
        read = (_, _, duration) => duration
      )
      .maximumSize(100)
      .build(p => cache.getIfPresent(p.swap).getOrElse(oneFrameClient.loadPair(p)))

  def getOneFrameData(pair: Rate.Pair): Either[Error, Rate] = {
    pair match {
      case _ if isValidPair(pair) => cache
        .get(pair)
        .flatMap(
          p =>
            p.get(pair)
              .toRight({
                OneFrameLookupFailed(s"Unable to retrieve data for pair = $pair, pls try again later")
              })
        )
      case _ => Left(BadPairProvided(s"Pair always should be different, has been provided $pair"))
    }

  }

  private def isValidPair(pair: Pair): Boolean = pair.to != pair.from
}
