package forex.services.rates.interpreters.oneframe

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price }
import io.circe.Decoder.decodeLocalDateTimeWithFormatter
import io.circe.generic.extras.Configuration
import io.circe.{ Decoder, DecodingFailure }
import org.http4s.circe.jsonOf
import org.http4s.{ EntityDecoder, QueryParamEncoder }

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object OneFrameProtocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val currencyDecoder: Decoder[Currency] = {
    Decoder.instance[Currency] { cursor =>
      println("Parsing cursor")
      Option(cursor)
        .map(c => c.value)
        .filter(v => v.isString)
        .flatMap(v => v.asString)
        .map(Currency.fromString)
        .get
        .asRight[DecodingFailure]
    }
  }

  implicit val priceDecoder: Decoder[Price] = {
    Decoder.instance[Price] { cursor =>
      Option(cursor)
        .map(c => c.value)
        .filter(v => v.isNumber)
        .flatMap(v => v.asNumber)
        .flatMap(n => n.toBigDecimal)
        .map(b => Price(b))
        .get
        .asRight[DecodingFailure]
    }
  }

  implicit val timestampDecoder: Decoder[LocalDateTime] = decodeLocalDateTimeWithFormatter(
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  )

  implicit val getOneFrameResponseDecoder: Decoder[GetOneFrameResponse] = Decoder[GetOneFrameResponse] { cursor =>
    for {
      from <- cursor.downField("from").as[Currency]
      to <- cursor.downField("to").as[Currency]
      price <- cursor.downField("price").as[Price]
      timestamp <- cursor.downField("time_stamp").as[LocalDateTime]
    } yield {
      GetOneFrameResponse(from, to, price, timestamp)
    }
  }

  implicit val getOneFrameResponseEntityDecoder: EntityDecoder[IO, List[GetOneFrameResponse]] =
    jsonOf[IO, List[GetOneFrameResponse]]

  implicit val queryParamEncoder: QueryParamEncoder[Pair] = OneFrameQueryParams.pairQueryParam

  case class GetOneFrameResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timeStamp: LocalDateTime,
  )
}
