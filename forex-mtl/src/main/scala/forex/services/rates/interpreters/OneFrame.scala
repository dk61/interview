package forex.services.rates.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.errors._
import forex.services.rates.interpreters.oneframe.OneFrameClient

class OneFrame[F[_]: Applicative] extends Algebra[F] {
  val oneFrameClient: OneFrameClient = new OneFrameClient
  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    oneFrameClient.getOneFrameData(pair).asRight[Error].pure[F]
  }
}
