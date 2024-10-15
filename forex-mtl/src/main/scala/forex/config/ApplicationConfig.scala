package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    currencyProvider: CurrencyProviderConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class CurrencyProviderConfig(
    host: String,
    port: Int,
    token: String
)
