package io.github.kirill5k.agg.enquiry

import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.Stream
import fs2.concurrent.{NoneTerminatedQueue, Queue}
import io.github.kirill5k.agg.common.errors.EnquiryNotFound

import java.util.UUID

trait EnquiryStore[F[_]] {
  def create(query: Query): F[EnquiryId]
  def get(id: EnquiryId): F[Enquiry]
  def exists(id: EnquiryId): F[Boolean]
  def complete(id: EnquiryId): F[Unit]
  def addQuote(id: EnquiryId)(quote: Quote): F[Unit]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}

final private class InMemoryEnquiryStore[F[_]: Concurrent](
    private val enquiries: Ref[F, Map[EnquiryId, Enquiry]],
    private val quotes: Ref[F, Map[EnquiryId, NoneTerminatedQueue[F, Quote]]]
) extends EnquiryStore[F] {

  override def create(query: Query): F[EnquiryId] =
    for {
      id    <- Concurrent[F].delay(EnquiryId(UUID.randomUUID().toString))
      _     <- enquiries.update(_ + (id -> Enquiry(id, "processing", query, Nil)))
      queue <- Queue.noneTerminated[F, Quote]
      _     <- quotes.update(_ + (id -> queue))
    } yield id

  override def get(id: EnquiryId): F[Enquiry] =
    enquiries.get.map(_.get(id)).flatMap {
      case Some(enquiry) => enquiry.pure[F]
      case None          => EnquiryNotFound(id).raiseError[F, Enquiry]
    }

  override def exists(id: EnquiryId): F[Boolean] =
    enquiries.get.map(_.keySet.contains(id))

  override def complete(id: EnquiryId): F[Unit] =
    for {
      enquiry <- get(id)
      _       <- enquiries.update(_ + (id -> enquiry.copy(status = "completed")))
      _       <- quotes.getAndUpdate(_.removed(id)).map(_(id)).flatMap(_.enqueue1(None))
    } yield ()

  override def addQuote(id: EnquiryId)(quote: Quote): F[Unit] =
    for {
      enquiry <- get(id)
      _       <- enquiries.update(_ + (id -> enquiry.copy(quotes = quote :: enquiry.quotes)))
      _       <- quotes.get.map(_(id)).flatMap(_.enqueue1(Some(quote)))
    } yield ()

  override def getQuotes(id: EnquiryId): Stream[F, Quote] =
    Stream
      .eval(enquiries.get)
      .flatMap { es =>
        es.get(id) match {
          case Some(enquiry) if enquiry.status == "completed" => Stream.emits(enquiry.quotes)
          case Some(_)                                        => Stream.eval(quotes.get).flatMap(_(id).dequeue)
          case None                                           => Stream.empty
        }
      }
}

object EnquiryStore {
  def inMemory[F[_]: Concurrent]: F[EnquiryStore[F]] = {
    def enquiries: F[Ref[F, Map[EnquiryId, Enquiry]]]                    = Ref.of(Map.empty)
    def quotes: F[Ref[F, Map[EnquiryId, NoneTerminatedQueue[F, Quote]]]] = Ref.of(Map.empty)
    (enquiries, quotes).mapN((e, q) => new InMemoryEnquiryStore[F](e, q))
  }
}
