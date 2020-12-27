package io.github.kirill5k.agg.enquiry

import cats.effect.Concurrent
import fs2.Stream

trait EnquiryService[F[_]] {
  def create(query: Query): F[EnquiryId]
  def exists(id: EnquiryId): F[Boolean]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}

final class LiveEnquiryService[F[_]: Concurrent](
    providerClient: ProviderClient[F],
    enquiryStore: EnquiryStore[F]
) extends EnquiryService[F] {
  override def create(query: Query): F[EnquiryId] = ???

  override def exists(id: EnquiryId): F[Boolean] = ???

  override def getQuotes(id: EnquiryId): Stream[F, Quote] = ???
}
