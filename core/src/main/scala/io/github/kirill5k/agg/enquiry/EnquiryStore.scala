package io.github.kirill5k.agg.enquiry

import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.Stream
import fs2.concurrent.{NoneTerminatedQueue, Queue}
import io.github.kirill5k.agg.common.errors.EnquiryNotFound

trait EnquiryStore[F[_]] {
  def save(enquiry: Enquiry): F[Unit]
  def get(id: EnquiryId): F[Enquiry]
  def exists(id: EnquiryId): F[Boolean]
  def complete(id: EnquiryId): F[Unit]
  def addQuote(id: EnquiryId)(quote: Quote): F[Unit]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}

final class InMemoryEnquiryStore[F[_]: Concurrent](
    private val enquiries: Ref[F, Map[EnquiryId, Enquiry]],
    private val quotes: Ref[F, Map[EnquiryId, NoneTerminatedQueue[F, Quote]]]
) extends EnquiryStore[F] {

  override def save(enquiry: Enquiry): F[Unit] =
    for {
      _     <- enquiries.update(_ + (enquiry.id -> enquiry))
      queue <- Queue.noneTerminated[F, Quote]
      _     <- quotes.update(_ + (enquiry.id -> queue))
    } yield ()

  override def get(id: EnquiryId): F[Enquiry] =
    enquiries.get.map(_.get(id)).flatMap {
      case Some(enquiry) => enquiry.pure[F]
      case None => EnquiryNotFound(id).raiseError[F, Enquiry]
    }

  override def exists(id: EnquiryId): F[Boolean] =
    enquiries.get.map(_.keySet.contains(id))

  override def complete(id: EnquiryId): F[Unit] =
    get(id).flatMap { enquiry =>
      enquiries.update(_ + (id -> enquiry.copy(status = "completed"))) *>
        quotes.get.map(_(id)).flatMap(_.enqueue1(None))
    }

  override def addQuote(id: EnquiryId)(quote: Quote): F[Unit] =
    get(id).flatMap { enquiry =>
      enquiries.update(_ + (id -> enquiry.copy(quotes = quote :: enquiry.quotes))) *>
        quotes.get.map(_(id)).flatMap(_.enqueue1(Some(quote)))
    }

  override def getQuotes(id: EnquiryId): Stream[F, Quote] =
    Stream
      .eval(quotes.get)
      .flatMap { qs =>
        qs.get(id).map(_.dequeue).getOrElse(Stream.empty)
      }
}
