package etc.rinha.app

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import etc.rinha.shared._
import org.http4s.Method._
import org.http4s.{ HttpApp, Request, Response, Uri }
import scala.concurrent.Future

object WarmUp {

  def service(
    app: HttpApp[IO],
    bulkSize: Int = 128
  )(implicit io: IORuntime): IO[Unit] = {

    def bodyOf(r: Response[IO]) = {
      val v = r.body.compile.toVector.unsafeRunSync()
      new String(v.toArray)
    }

    def getIO(uri: Uri) =
      app.run(Request(method = GET, uri))

    def postIO(uri: Uri, body: Option[String]) = {
      val req: Request[IO] = body match {
        case None => Request(method = POST, uri)
        case Some(b) => Request(method = POST, uri, body = fs2.Stream.emits(b.getBytes))
      }
      app.run(req)
    }

    val impl = new WarmUpProtocol {
      override def get(url: String): Future[(Int, String)] =
        getIO(Uri.unsafeFromString(url))
          .map { r => (r.status.code, bodyOf(r)) }
          .unsafeToFuture()

      override def post(url: String, body: Option[String]): Future[(Int, String)] = {
        postIO(Uri.unsafeFromString(url), body)
          .map { r => (r.status.code, bodyOf(r)) }
          .unsafeToFuture()
      }
    }

    val f = WarmUpService.run(impl, bulkSize)(io.compute)
    IO.fromFuture(IO.pure(f))
  }
}