package etc.rinha.app

import cats.effect._
import cats.implicits.toSemigroupKOps
import etc.rinha.app.service.PessoaService
import etc.rinha.shared.PessoaIn
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{ EntityDecoder, Header, HttpApp, HttpRoutes, ParseFailure, QueryParamDecoder, Response, Status }
import org.typelevel.ci.CIString
import scala.util.{ Failure, Success, Try }

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

  case class NonEmptyString(value: String) extends AnyVal
  implicit val yearQueryParamDecoder: QueryParamDecoder[NonEmptyString] =
    QueryParamDecoder[String].emap { str =>
      Either.cond(
        !str.trim.isEmpty,
        NonEmptyString(str),
        ParseFailure("required param", "required param")
      )
    }
  private object TermParamMatcher extends OptionalQueryParamDecoderMatcher[NonEmptyString]("t")

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
              case Failure(t) =>
                UnprocessableEntity(t.getMessage)
              case Success(_) =>
                Created(id.toString, Header.Raw(CIString("Location"), s"/pessoas/$id"))
            }
          } yield res
        ).recover {
          t => {
            //t.printStackTrace() // debug
            Response(status = Status.UnprocessableEntity)
          }
        }

      case GET -> Root / "pessoas" / UUIDVar(id) =>
        svc.getPessoa(id).flatMap {
          case Some(p) => Ok(p.asJson)
          case None    => NotFound()
        }

      case GET -> Root / "pessoas" :? TermParamMatcher(t) =>
        t match {
          case Some(NonEmptyString(s))  =>
            svc.findPessoas(s).flatMap { p =>
              Ok(p.asJson)
            }
          case _ => BadRequest()
        }

      case GET -> Root / s"contagem-pessoas" =>
        svc.count.flatMap { p =>
          Ok(p.asJson)
        }
    }
  }


}
