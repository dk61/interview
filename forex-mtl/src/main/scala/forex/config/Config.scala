package forex.config

import cats.effect.Sync
import fs2.Stream
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Config {

  def stream[F[_]: Sync]: Stream[F, ApplicationConfig] = {
    Stream.eval(Sync[F].delay(read))
  }

  def read :ApplicationConfig = {
    ConfigSource.default.at(getPath).loadOrThrow[ApplicationConfig]
  }

  private def getPath: String = {
    resolveEnv
      .map(s => s"$s-app")
      .get
  }

  private def resolveEnv: Option[String] = {
    Option(System.getenv("APP_ENV"))
      .orElse(Some("local"))
  }
}
