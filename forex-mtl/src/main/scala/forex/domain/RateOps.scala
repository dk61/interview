package forex.domain

import forex.domain.Rate.Pair

object RateOps {
  implicit class PairOps(val pair: Pair) extends AnyVal {
    def swap: Pair = Pair(from = pair.to, to = pair.from)
  }
}
