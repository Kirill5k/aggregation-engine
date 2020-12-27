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

  private def randInt(from: Int, to: Int): Int =
    from + rand.nextInt(to - from)

  private def queryProvider(providerName: String, query: Query): F[Quote] =
    L.info(s"querying provider $providerName for query by ${query.firstName} ${query.lastName}") *>
      T.sleep(randInt(5000, 1000).millis) *>
      F.delay(Quote(providerName, BigDecimal(randInt(10, 20) + rand.nextDouble())))

  override def queryAll(query: Query): Stream[F, Quote] =
    Stream
      .emits(providers)
      .map(p => Stream.eval(queryProvider(p, query)))
      .parJoinUnbounded
      .evalTap(q => L.info(s"received quote from ${q.providerName} for query by ${query.firstName} ${query.lastName}"))
}

object ProviderClient {
  def mock[F[_]: Concurrent: Timer: Logger]: F[ProviderClient[F]] =
    Concurrent[F].delay(new MockProviderClient[F])
}
