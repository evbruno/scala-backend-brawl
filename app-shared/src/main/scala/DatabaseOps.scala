package etc.rinha.shared

import java.sql._
import java.util.UUID
import scala.util._

object DatabaseOps {

  def select(termo: String)(implicit conn: Connection): List[PessoaOut] =
    Using(conn.prepareStatement(SELECT_TERMO)) { stmt =>
      val value = s"%$termo%"
      stmt.setString(1, value)
      val rs = stmt.executeQuery()
      rs
        .toLazyList()
        .map(fromRS(_))
        .toList
    }.fold(
      fa => {
        fa.printStackTrace()
        Nil
      },
      identity
    )

  def findById(id: UUID)(implicit conn: Connection): Option[PessoaOut] =
    Using(conn.prepareStatement(SELECT_ID)) { stmt =>
      stmt.setObject(1, id)
      val rs = stmt.executeQuery()
      if (!rs.next())
        None
      else
        Some(fromRS(rs))
    }.fold(
      fa => {
        fa.printStackTrace()
        None
      },
      identity
    )

  private def fromRS(rs: ResultSet): PessoaOut = {
    PessoaOut(
      id = rs.getObject(1, classOf[UUID]),
      apelido = rs.getString(2),
      nome = rs.getString(3),
      nascimento = rs.getString(4),
      stack = buildStack(Option(rs.getString(5)))
    )
  }

  private def buildStack(s: Option[String]) =
    s.map(
      _.split(", ").toList
    ).getOrElse(Nil)

  def insert(id: UUID, p: PessoaIn)(implicit conn: Connection): Either[Throwable, PessoaOut] = {
    Using(conn.prepareStatement(INSERT)) { stmt =>
      stmt.setObject(1, id)
      stmt.setString(2, p.apelido)
      stmt.setString(3, p.nome)
      stmt.setString(4, p.nascimento)

      val stackStr = p.stack.map(_.mkString(", ")).getOrElse(null)
      stmt.setString(5, stackStr)

      stmt.executeUpdate()
    }.fold(
      t =>
        if (t.getMessage.contains("duplicate key value violates unique constraint")) {
//          t.printStackTrace()
          Left(ApelidoEmUso(p))
        } else
          Left(t),
      _ => Right(PessoaOut(id, p))
    )
  }

  def count(implicit conn: Connection): Option[Int] =
    Using(conn.createStatement()) { stmt =>
      val rs = stmt.executeQuery(COUNT)
      rs.next()
      rs.getInt(1)
    }.toOption

  def truncate()(implicit conn: Connection): Try[Int] =
    Using(conn.createStatement()) {
      _.executeUpdate(TRUNCATE)
    }

  def deleteApelidoLike(str: String)(implicit conn: Connection): Try[Int] =
    Using(conn.prepareStatement(DELETE)) { stmt =>
      val value = s"%$str%"
      stmt.setString(1, value)
      stmt.executeUpdate()
    }

  // constants

  private val INSERT =
    """INSERT INTO pessoas (id, apelido, nome, nascimento, stack)
      |VALUES (?, ?, ?, ?, ?)""".stripMargin

  private val COUNT = "SELECT COUNT(*) FROM pessoas"

  private val SELECT_TERMO =
    "SELECT id, apelido, nascimento, nome, stack from pessoas WHERE busca_trgm like ?"

  private val SELECT_ID = "SELECT id, apelido, nascimento, nome, stack from pessoas WHERE id = ?"

  private val TRUNCATE = "TRUNCATE TABLE pessoas"

  private val DELETE = "DELETE from PESSOAS WHERE apelido like ?"

  // Implicits

  class ResultSetToStream(source: ResultSet) {
    // will this blow up?
    def toLazyList(close: Boolean = true): LazyList[ResultSet] = {
      def rec(rs: ResultSet): LazyList[ResultSet] =
        if (rs.next())
          rs #:: rec(rs)
        else {
          if (close)
            Try(rs.close())
          LazyList.empty
        }

      rec(source)
    }
  }

  implicit def rsToStream(rs: ResultSet): ResultSetToStream =
    new ResultSetToStream(rs)
}
