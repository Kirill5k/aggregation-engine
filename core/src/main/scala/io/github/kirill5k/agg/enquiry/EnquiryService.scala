package io.github.kirill5k.agg.enquiry

import cats.effect.Concurrent
import cats.effect.implicits._
import cats.implicits._
import fs2.Stream

trait EnquiryService[F[_]] {
  def create(query: Query): F[EnquiryId]
  def get(id: EnquiryId): F[Enquiry]
  def exists(id: EnquiryId): F[Boolean]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}

private final class LiveEnquiryService[F[_]](
    private val providerClient: ProviderClient[F],
    private val enquiryStore: EnquiryStore[F]
)(implicit
    val F: Concurrent[F]
) extends EnquiryService[F] {

  override def create(query: Query): F[EnquiryId] =
    for {
      id  <- enquiryStore.create(query)
      _ <- Stream
        .bracket(F.pure(id))(enquiryStore.complete)
        .flatMap(_ => providerClient.queryAll(query).evalTap(enquiryStore.addQuote(id)))
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

  def make[F[_]](
      providerClient: ProviderClient[F],
      enquiryStore: EnquiryStore[F]
  )(implicit F: Concurrent[F]): F[EnquiryService[F]] =
    F.delay(new LiveEnquiryService[F](providerClient, enquiryStore))
}
