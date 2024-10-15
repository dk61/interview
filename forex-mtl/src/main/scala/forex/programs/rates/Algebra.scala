package forex.programs.rates

import forex.domain.Rate
import forex.programs.rates.errors.ProgramError

trait Algebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[ProgramError Either Rate]
}
