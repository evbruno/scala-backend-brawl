package etc.rinha.app

import cats.effect.{ IO, Ref }
import etc.rinha.app.service.PessoaService
import etc.rinha.shared.Payloads
import java.net.InetAddress.getLocalHost
import scala.concurrent.duration._
import cats.syntax.all._

// dumb "testing"
// should help grraalvm agent to detect "running paths"
// TODO move to shared so other impls could use the same "tests"
object WarmUp {

  case class WarmUpException(m: String) extends RuntimeException

  def apply(
   svc: PessoaService[IO],
   health: Ref[IO, Boolean],
   bulk: Int = 128
 ): IO[Unit] =
    for {
      _ <- IO.println("... starting warm up")
      t <- job(svc, bulk).timed
      _ <- health.update(_ => true)
      _ <- IO.println(s"... server is ready (wup took ${t._1.toMillis} millis)!") // FIXME move
    } yield ()

  private def job(svc: PessoaService[IO], bulk: Int = 128) =
    for {
      prefix <- IO.delay(s"fooo_${getLocalHost.getHostName}_")
      next   <- svc.count.map(_ + 1)
      id0    <- IO.randomUUID
      _      <- svc.save(Payloads.randomPessoaIn(prefix + next, Some(id0)))
      outs   <- svc.findPessoas(prefix)
      _      <- IO.raiseWhen(outs.isEmpty)(WarmUpException(s"search failed #findPessoas($prefix)"))
      out0   <- svc.getPessoa(id0)
      _      <- IO.raiseWhen(out0.isEmpty)(WarmUpException(s"search failed #getPessoa($id0)"))
      del    <- svc.deleteApelidoLike(prefix)
      _      <- IO.raiseWhen(del < 1)(WarmUpException(s"delete failed (ret=$del)"))
      _      <- IO.println(s"bulk insert starting ($bulk entries)") >> IO.sleep(500.millis)

      ios    = (1 to bulk).map(i => svc.save(Payloads.randomPessoaIn(prefix + i + "_")))
      saves  <- ios.toList.parSequence.timed
      _      <- IO.println(s"bulk insert took ${saves._1.toMillis} ms (${saves._2.count(_.isFailure)} errors)")
      del    <- svc.deleteApelidoLike(prefix)
      _      <- IO.raiseWhen(del < 1)(WarmUpException(s"delete failed (ret=$del)"))

    } yield ()


}
