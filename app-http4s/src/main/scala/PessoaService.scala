package etc.rinha.app

import cats.Monad
import cats.effect.Ref
import cats.effect.kernel.Sync
import etc.rinha.shared._
import java.util.UUID
import scala.util._
import cats.syntax.all._
import etc.rinha.app.Http4sApp.AppConfig
import java.sql.{ Connection, DriverManager }
import javax.sql.DataSource

object service {
  
  trait PessoaService[F[_]] {
    def findPessoas(t: String): F[List[PessoaOut]]
    def save(id: UUID, p: PessoaIn): F[Try[PessoaOut]]
    def getPessoa(id: UUID): F[Option[PessoaOut]]
    def count: F[Int]
    def deleteApelidoLike(str: String): F[Unit]
  }

  private[service] class InMemory[F[_]: Sync](
    pessoasMap: Ref[F, Map[UUID, PessoaOut]]
  ) extends PessoaService[F] {

    def findPessoas(t: String): F[List[PessoaOut]] =
      pessoasMap.get.map(
        _.values.collect {
          case p if p.toString.toLowerCase.contains(t.toLowerCase) => p
        }.toList
      )

    def save(id: UUID, p: PessoaIn): F[Try[PessoaOut]] = {
      findessoa(p).flatMap {
        _ match {
          case None =>
            val out = PessoaOut(id, p)
            pessoasMap.update(_ + (id -> out)).flatMap { _ =>
              Sync[F].delay(Success(out))
            }
          case Some(p) =>
            throw new RuntimeException(s"Pessoa ja cadastrada: ${p.apelido}")
        }

      }
    }

    private def findessoa(in: PessoaIn): F[Option[PessoaOut]] =
      pessoasMap.get.map(
        _.values.find(_.apelido == in.apelido)
      )

    def getPessoa(id: UUID): F[Option[PessoaOut]] =
      pessoasMap.get.map(_.get(id))

    def count: F[Int] =
      pessoasMap.get.map(_.size)

    def deleteApelidoLike(str: String): F[Unit] =
      // that's ok 🥹
      pessoasMap.set(Map.empty)
  }

  private[service] class InDatabase[F[_] : Sync](
    conn : Connection
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

    def deleteApelidoLike(str: String): F[Unit] =
      Sync[F].blocking(
        DatabaseOps.deleteApelidoLike(str)
      )
  }

  private[service] class ByPass[F[_] : Monad] extends PessoaService[F] {

    def findPessoas(t: String): F[List[PessoaOut]] = Monad[F].pure(Nil)

    def save(id: UUID, p: PessoaIn): F[Try[PessoaOut]] =
      Monad[F].pure(Success(PessoaOut(id, p)))

    def getPessoa(id: UUID): F[Option[PessoaOut]] = Monad[F].pure(None)

    def count: F[Int] = Monad[F].pure(-1)

    def deleteApelidoLike(str: String): F[Unit] = Monad[F].unit
  }

  def inMemory[F[_]: Sync](m: Ref[F, Map[UUID, PessoaOut]]): PessoaService[F] =
    new InMemory[F](m)

  def inDatabase[F[_]: Sync](cfg: AppConfig): PessoaService[F] =
    new InDatabase[F](makeConnection(cfg.newDatasource))

  def byPass[F[_]: Monad]: PessoaService[F] = new ByPass[F]

  private def makeConnection(ds: DataSource): Connection = {
    val conn = ds.getConnection
    createTable(conn)
      .fold(fa => throw fa, _ => conn)
  }

  private def createTable(conn: Connection) =
    DatabaseOps.ddl()(conn)

}
