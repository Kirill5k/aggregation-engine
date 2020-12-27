package io.github.kirill5k.agg.enquiry

import cats.effect.{Concurrent, Sync}
import cats.effect.implicits._
import cats.implicits._
import fs2.Stream

import java.util.UUID

trait EnquiryService[F[_]] {
  def get(id: EnquiryId): F[EnquiryId]
  def create(query: Query): F[EnquiryId]
  def exists(id: EnquiryId): F[Boolean]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}

final class LiveEnquiryService[F[_]](
    private val providerClient: ProviderClient[F],
    private val enquiryStore: EnquiryStore[F]
)(implicit
    val F: Concurrent[F]
) extends EnquiryService[F] {

  override def create(query: Query): F[EnquiryId] =
    for {
      id <- F.delay(EnquiryId(UUID.randomUUID().toString))
      _  <- enquiryStore.save(Enquiry(id, "processing", query, Nil))
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

  override def get(id: EnquiryId): F[EnquiryId] =
    enquiryStore.get(id)
}

object EnquiryService {

  def make[F[_]](
      providerClient: ProviderClient[F],
      enquiryStore: EnquiryStore[F]
  )(implicit F: Concurrent[F]): F[EnquiryService[F]] =
    F.delay(new LiveEnquiryService[F](providerClient, enquiryStore))
}
