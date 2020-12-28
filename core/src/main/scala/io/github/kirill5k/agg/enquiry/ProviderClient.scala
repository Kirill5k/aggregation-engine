package io.github.kirill5k.agg.enquiry

import cats.effect.{Concurrent, Timer}
import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

import scala.util.Random
import scala.concurrent.duration._

trait ProviderClient[F[_]] {
  def queryAll(query: Query): Stream[F, Quote]
}

private final class MockProviderClient[F[_]: Timer](implicit
    val F: Concurrent[F],
    val T: Timer[F],
    val L: Logger[F]
) extends ProviderClient[F] {
  private val rand = Random
  private val providers = List("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9")

  private def queryProvider(providerName: String, query: Query): F[Quote] =
    L.info(s"querying provider $providerName for query by ${query.firstName} ${query.lastName}") *>
      T.sleep(rand.nextInt(15000).millis) *>
      F.delay(Quote(providerName, BigDecimal(rand.nextInt(20) + rand.nextDouble()))) <*
      L.info(s"received quote from $providerName for query by ${query.firstName} ${query.lastName}")

  override def queryAll(query: Query): Stream[F, Quote] =
    Stream
      .emits(providers)
      .covary[F]
      .map(p => Stream.eval(queryProvider(p, query)))
      .parJoinUnbounded
}

object ProviderClient {
  def mock[F[_]: Concurrent: Timer: Logger]: F[ProviderClient[F]] =
    Concurrent[F].delay(new MockProviderClient[F])
}
