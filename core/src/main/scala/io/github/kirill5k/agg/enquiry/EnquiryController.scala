package io.github.kirill5k.agg.enquiry

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.github.kirill5k.agg.enquiry.EnquiryController.ErrorResponse
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.kirill5k.agg.common.JsonCodecs
import org.http4s.circe._
import org.http4s.{HttpRoutes, MessageFailure, Response}
import org.http4s.dsl.Http4sDsl

final class EnquiryController[F[_]: Sync](implicit l: Logger[F]) extends Http4sDsl[F] with JsonCodecs {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "enquiries" =>
      withErrorHandling {
        for {
          query <- req.as[Query]
          res   <- Ok(query.asJson)
        } yield res
      }
  }

  private def withErrorHandling(response: => F[Response[F]]): F[Response[F]] =
    response.handleErrorWith {
      case error: MessageFailure =>
        l.error(error)(s"error parsing json}") *>
          BadRequest(ErrorResponse(error.getMessage()).asJson)
      case error =>
        l.error(error)(s"unexpected error") *>
          InternalServerError(ErrorResponse(error.getMessage()).asJson)
    }
}

object EnquiryController {
  final case class ErrorResponse(message: String)
}
