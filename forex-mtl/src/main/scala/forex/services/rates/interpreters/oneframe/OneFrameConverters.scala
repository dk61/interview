package forex.services.rates.interpreters.oneframe

import forex.domain.Rate.Pair
import forex.domain.{Rate, Timestamp}

import java.time.{OffsetDateTime, ZoneOffset}

object OneFrameConverters {
  import OneFrameProtocol._

  private[oneframe] implicit class GetOneFrameResponseOps(val getOneFrameResponse: GetOneFrameResponse) extends AnyVal {
    def asRate: Rate =
      Rate(
        pair = Pair(from = getOneFrameResponse.from, to = getOneFrameResponse.to),
        price = getOneFrameResponse.price,
        timestamp = Timestamp(
          OffsetDateTime
            .of(getOneFrameResponse.timeStamp, ZoneOffset.UTC)
            .toInstant
            .atOffset(OffsetDateTime.now.getOffset)
        )
      )
  }
}
