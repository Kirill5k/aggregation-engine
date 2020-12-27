package io.github.kirill5k.agg

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.kirill5k.agg.common.config.AppConfig
import io.github.kirill5k.agg.enquiry.{EnquiryController, EnquiryService, EnquiryStore, ProviderClient}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router

import scala.concurrent.ExecutionContext

object Application extends IOApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config            <- Blocker[IO].use(AppConfig.load[IO]) <* logger.info("loaded config")
      providerClient    <- ProviderClient.mock[IO]
      enquiryStore      <- EnquiryStore.inMemory[IO]
      enquiryService    <- EnquiryService.make[IO](providerClient, enquiryStore)
      enquiryController <- EnquiryController.make[IO](enquiryService)
      _ <- BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(config.server.port, config.server.host)
        .withHttpApp(Router("api" -> enquiryController.routes).orNotFound)
        .serve
        .compile
        .drain
    } yield ExitCode.Success
}
