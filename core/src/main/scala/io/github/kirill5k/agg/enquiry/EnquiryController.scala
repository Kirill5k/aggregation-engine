package io.github.kirill5k.agg.enquiry

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.kirill5k.agg.common.JsonCodecs
import io.github.kirill5k.agg.common.errors.EnquiryNotFound
import io.github.kirill5k.agg.enquiry.EnquiryController.{CreateEnquiryRequest, CreateEnquiryResponse, ErrorResponse}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, MessageFailure, Response}

final class EnquiryController[F[_]: Sync](
    service: EnquiryService[F]
)(implicit
    l: Logger[F]
) extends Http4sDsl[F] with JsonCodecs {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "enquiries" =>
      withErrorHandling {
        for {
          reqBody <- req.as[CreateEnquiryRequest]
          id      <- service.create(reqBody.query)
          res     <- Ok(CreateEnquiryResponse(id))
        } yield res
      }
    case GET -> Root / "enquiries" / id / "quotes" =>
      withErrorHandling {
        Ok(service.getQuotes(EnquiryId(id)))
      }
  }

  private def withErrorHandling(response: => F[Response[F]]): F[Response[F]] =
    response.handleErrorWith {
      case error: EnquiryNotFound =>
        l.error(error.message) *>
          NotFound(ErrorResponse(error.message).asJson)
      case error: MessageFailure =>
        l.error(error)("error parsing json") *>
          BadRequest(ErrorResponse(error.getMessage()).asJson)
      case error =>
        l.error(error)("unexpected error") *>
          InternalServerError(ErrorResponse(error.getMessage()).asJson)
    }
}

object EnquiryController {

  final case class CreateEnquiryRequest(
      query: Query
  )

  final case class CreateEnquiryResponse(
      enquiryId: EnquiryId
  )

  final case class ErrorResponse(message: String)
}
