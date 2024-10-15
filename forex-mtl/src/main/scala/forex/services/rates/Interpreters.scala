package forex.services.rates

import cats.Applicative
import interpreters._

object Interpreters {
  def oneFrame[F[_]: Applicative]: Algebra[F] = new OneFrame[F]()
}
