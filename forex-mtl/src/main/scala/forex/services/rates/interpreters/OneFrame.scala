package forex.services.rates.interpreters

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.errors._
import forex.services.rates.interpreters.oneframe.{OneFrameCachedClient, OneFrameClient}

class OneFrame[F[_]: Applicative] extends Algebra[F] {
  val oneFrameClient: OneFrameCachedClient = new OneFrameCachedClient(new OneFrameClient)
  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    oneFrameClient.getOneFrameData(pair).pure[F]
  }
}
