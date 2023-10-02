package etc.rinha.app

import cats.Monad
import cats.effect.Ref
import cats.effect.kernel.Sync
import cats.syntax.all._
import etc.rinha.shared._
import java.sql.Connection
import java.util.UUID
import scala.util._

object service {

  trait PessoaService[F[_]] {
    def findPessoas(t: String): F[List[PessoaOut]]

    def save(id: UUID, p: PessoaIn): F[Try[PessoaOut]]

    def save(in: (UUID, PessoaIn)): F[Try[PessoaOut]] =
      save(in._1, in._2)

    def getPessoa(id: UUID): F[Option[PessoaOut]]

    def count: F[Int]

    def deleteApelidoLike(str: String): F[Int]
  }

  private[service] class InMemory[F[_] : Sync](
    mem: Ref[F, Map[UUID, PessoaOut]]
  ) extends PessoaService[F] {

    def findPessoas(t: String): F[List[PessoaOut]] =
      mem.get.map(
        _.values.collect {
          case p if p.toString.toLowerCase.contains(t.toLowerCase) => p
        }.toList
      )

    def save(id: UUID, p: PessoaIn): F[Try[PessoaOut]] = {
      findessoa(p).flatMap {
        _ match {
          case None    =>
            val out = PessoaOut(id, p)
            mem.update(_ + (id -> out)).flatMap { _ =>
              Sync[F].delay(Success(out))
            }
          case Some(_) =>
            throw ApelidoEmUso(p)
        }

      }
    }

    private def findessoa(in: PessoaIn): F[Option[PessoaOut]] =
      mem.get.map(
        _.values.find(_.apelido == in.apelido)
      )

    def getPessoa(id: UUID): F[Option[PessoaOut]] =
      mem.get.map(_.get(id))

    def count: F[Int] =
      mem.get.map(_.size)

    def deleteApelidoLike(str: String): F[Int] = {
      // that's ok 🥹
      mem.set(Map.empty)
      ???
    }
  }

  private[service] class InDatabase[F[_] : Sync](
    conn: Connection
  ) extends PessoaService[F] {

    implicit val _conn: Connection = conn

    def findPessoas(t: String): F[List[PessoaOut]] =
      Sync[F].blocking(
        DatabaseOps.select(t)
      )

    def save(id: UUID, p: PessoaIn): F[Try[PessoaOut]] =
      Sync[F].blocking(
        DatabaseOps.insert(id, p).toTry // FIXME
      )

    def getPessoa(id: UUID): F[Option[PessoaOut]] =
      Sync[F].delay(
        DatabaseOps.findById(id)
      )

    def count: F[Int] =
      Sync[F].delay(
        DatabaseOps.count.getOrElse(-1)
      )

    def deleteApelidoLike(str: String): F[Int] =
      Sync[F].blocking(
        DatabaseOps.deleteApelidoLike(str).getOrElse(-1)
      )
  }

  private[service] class ByPass[F[_] : Monad] extends PessoaService[F] {

    def findPessoas(t: String): F[List[PessoaOut]] = Monad[F].pure(Nil)

    def save(id: UUID, p: PessoaIn): F[Try[PessoaOut]] =
      Monad[F].pure(Success(PessoaOut(id, p)))

    def getPessoa(id: UUID): F[Option[PessoaOut]] = Monad[F].pure(None)

    def count: F[Int] = Monad[F].pure(-1)

    def deleteApelidoLike(str: String): F[Int] = Monad[F].pure(42)
  }

  object ByPass {
    def apply[F[_] : Monad]: PessoaService[F] = new ByPass[F]
  }

  object InMemory {
    def apply[F[_] : Sync](mem: Ref[F, Map[UUID, PessoaOut]]): PessoaService[F] =
      new InMemory[F](mem)
  }

  object InDatabase {
    def apply[F[_] : Sync](conn: Connection): PessoaService[F] =
      new InDatabase[F](conn)
  }

}
