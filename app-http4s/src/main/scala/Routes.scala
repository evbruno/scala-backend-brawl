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

object Routes {

  def api(href: Ref[IO, Boolean], svc: PessoaService[IO]): HttpApp[IO] =
    (pessoasRoute(svc) <+> healthRoute(href)).orNotFound

  private def healthRoute(href: Ref[IO, Boolean]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "health" =>
        href.get.flatMap { h =>
          if (h) Ok()
          else BadRequest()
        }

      // update readiness (toggle)
      case POST -> Root / "health" =>
        href.updateAndGet(!_).flatMap { h =>
          Ok(s"health toggled => $h")
        }
    }

  private object TermParamMatcher extends QueryParamDecoderMatcher[String]("t")

  private val servicePath = "pessoas"

  private def pessoasRoute(svc: PessoaService[IO]): HttpRoutes[IO] = {
    implicit val decIn: EntityDecoder[IO, PessoaIn] =
      jsonOf[IO, PessoaIn]


    HttpRoutes.of[IO] {
      case req@POST -> Root / servicePath =>
        (
          for {
            pesIn <- req.as[PessoaIn]
            id <- IO.randomUUID
            maybe <- svc.save(id, pesIn)
            res <- maybe match {
              case Failure(t) => BadRequest(t.getMessage)
              case Success(_) =>
                Created(id.toString, Header.Raw(CIString("Location"), s"/$servicePath/$id"))
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

      case GET -> Root / servicePath / UUIDVar(id) =>
        svc.getPessoa(id).flatMap {
          case Some(p) => Ok(p.asJson)
          case None    => NotFound()
        }

      case GET -> Root / servicePath :? TermParamMatcher(t) =>
        svc.findPessoas(t).flatMap { p =>
          Ok(p.asJson)
        }

      case GET -> Root / s"contagem-$servicePath" =>
        svc.count.flatMap { p =>
          Ok(p.asJson)
        }
    }
  }


}
