package io.github.kirill5k.agg.common

import cats.Applicative
import cats.effect.Sync
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import io.github.kirill5k.agg.enquiry.EnquiryId
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}


object json extends JsonCodecs {

}

trait JsonCodecs {

  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
  implicit def deriveEntityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A]        = jsonOf[F, A]

  implicit val eidEncoder: Encoder[EnquiryId] = deriveUnwrappedEncoder
  implicit val eidDecoder: Decoder[EnquiryId] = deriveUnwrappedDecoder
}

