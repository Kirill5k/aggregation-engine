package io.github.kirill5k.agg.enquiry

import fs2.Stream

trait ProviderClient[F[_]] {
  def queryAll(query: Query): Stream[F, Quote]
}
