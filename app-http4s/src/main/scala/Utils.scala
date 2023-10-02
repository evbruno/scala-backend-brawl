package etc.rinha

import cats.Monad
import cats.effect.Temporal
import cats.implicits._
import scala.concurrent.duration._

object Utils {

  def retrier[F[_] : Temporal : Monad, A](
    f: F[A],
    onError: (Throwable, FiniteDuration) => F[Unit]
  ): F[A] = {
    val max = 5

    def rec(step: Int): F[A] = {
      f.handleErrorWith {
        case t: Throwable if step < max =>
          val s = (step << 8).millis
          onError(t, s) >> Temporal[F].sleep(s) >> rec(step + 1)
        case t                          =>
          Temporal[F].raiseError[A](t)
      }
    }

    rec(1)
  }

}
