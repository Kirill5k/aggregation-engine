package io.github.kirill5k.agg.enquiry

import cats.effect.{Concurrent, Timer}
import cats.effect.implicits._
import cats.implicits._
import fs2.Stream

trait EnquiryService[F[_]] {
  def create(query: Query): F[EnquiryId]
  def get(id: EnquiryId): F[Enquiry]
  def exists(id: EnquiryId): F[Boolean]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}

final private class LiveEnquiryService[F[_]: Concurrent](
    private val providerClient: ProviderClient[F],
    private val enquiryStore: EnquiryStore[F]
) extends EnquiryService[F] {

  override def create(query: Query): F[EnquiryId] =
    for {
      id <- enquiryStore.create(query)
      _ <- providerClient
        .queryAll(query)
        .evalMap(enquiryStore.addQuote(id))
        .onFinalize(enquiryStore.complete(id))
        .compile
        .drain
        .start
    } yield id

  override def exists(id: EnquiryId): F[Boolean] =
    enquiryStore.exists(id)

  override def getQuotes(id: EnquiryId): Stream[F, Quote] =
    enquiryStore.getQuotes(id)

  override def get(id: EnquiryId): F[Enquiry] =
    enquiryStore.get(id)
}

object EnquiryService {

  def make[F[_]: Timer: Concurrent](
      providerClient: ProviderClient[F],
      enquiryStore: EnquiryStore[F]
  ): F[EnquiryService[F]] =
    Concurrent[F].delay(new LiveEnquiryService[F](providerClient, enquiryStore))
}
