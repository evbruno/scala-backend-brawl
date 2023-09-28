package etc.rinha.app

import cats.effect._
import cats.implicits.toSemigroupKOps
import etc.rinha.app.service.PessoaService
import etc.rinha.shared.PessoaIn
import fs2.text.utf8
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{ EntityDecoder, Header, HttpApp, HttpRoutes, Response, Status }
import org.typelevel.ci.CIString
import scala.util.{ Failure, Success }

object AppRoutes {

  def api(href: Ref[IO, Boolean], svc: PessoaService[IO]): HttpApp[IO] =
    (
      pessoasRoute(svc) <+> healthRoute(href)
      ).orNotFound

  private def healthRoute(href: Ref[IO, Boolean]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "health" =>
        href.get.flatMap { h =>
          if (h) Ok()
          else BadRequest()
        }

      // force readiness (toggle)
      case POST -> Root / "health" =>
        href.updateAndGet(!_).flatMap { h =>
          Ok(s"health toggled => $h")
        }
    }

  private object TermParamMatcher extends QueryParamDecoderMatcher[String]("t")

  private def pessoasRoute(svc: PessoaService[IO]): HttpRoutes[IO] = {
    implicit val decIn: EntityDecoder[IO, PessoaIn] =
      jsonOf[IO, PessoaIn]

    HttpRoutes.of[IO] {
      case req@POST -> Root / "pessoas" =>
        (
          for {
            pesIn <- req.as[PessoaIn]
            id <- IO.randomUUID
            maybe <- svc.save(id, pesIn)
            res <- maybe match {
              case Failure(t) => BadRequest(t.getMessage)
              case Success(_) =>
                Created(
                  s"${pesIn.apelido} criado",
                  Header.Raw(CIString("Location"), s"/pessoas/$id")
                )
            }
          } yield res
          ).recover {
          t => {
            //t.printStackTrace() // debug
            Response(
              status = Status.BadRequest,
              body = fs2.Stream(s"""{"error": "${t.getMessage}"}""").through(utf8.encode)
            )
          }
        }

      case GET -> Root / "pessoas" / UUIDVar(id) =>
        svc.getPessoa(id).flatMap {
          case Some(p) => Ok(p.asJson)
          case None    => NotFound()
        }

      case GET -> Root / "pessoas" :? TermParamMatcher(t) =>
        svc.findPessoas(t).flatMap { p =>
          Ok(p.asJson)
        }

      case GET -> Root / "contagem-pessoas" =>
        svc.count.flatMap { p =>
          Ok(p.asJson)
        }
    }
  }


}
