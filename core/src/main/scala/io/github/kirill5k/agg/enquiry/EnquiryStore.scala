package io.github.kirill5k.agg.enquiry

import fs2.Stream

trait EnquiryStore[F[_]] {
  def save(enquiry: Enquiry): F[Unit]
  def exists(id: EnquiryId): F[Boolean]
  def complete(id: EnquiryId): F[Unit]
  def addQuote(id: EnquiryId)(quote: Quote): F[Unit]
  def getQuotes(id: EnquiryId): Stream[F, Quote]
}
