package forex.programs.rates

import forex.services.rates.errors.{Error => RatesServiceError}

object errors {

  sealed trait ProgramError extends Exception

  sealed trait ServerError extends ProgramError
  object ServerError extends ServerError {
    final case class RateLookupFailed(detailedMessage: String) extends ServerError
  }

  sealed trait ClientError extends ProgramError
  object ClientError extends ClientError {
    final case class BadInputParameters(detailedMessage: String) extends ClientError
  }

  def toProgramError(error: RatesServiceError): ProgramError = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => ServerError.RateLookupFailed(msg)
    case RatesServiceError.BadPairProvided(msg) => ClientError.BadInputParameters(msg)
  }
}
