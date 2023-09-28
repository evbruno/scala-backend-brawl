package etc.rinha.shared

import scala.concurrent.{ ExecutionContext, Future }

// internal request/response
// works as a jvm warmup and graalvm agent info collector
object WarmUpService {

  def run(
    impl: WarmUpProtocol,
    bulkSize: Int = 128
  )(implicit ec: ExecutionContext): Future[Unit] = {
    import impl._

    for {
      r <- get("/health")
      _ <- verify(r._1 == BadRequest)

      r <- get("/not-found")
      _ <- verify(r._1 == NotFound)

      r <- post("/pessoas", Some("{}"))
      _ <- verify(r._1 == BadRequest)

      r <- post("/pessoas", payload("fooo_1"))
      _ <- verify(r._1 == Created && r._2 == "fooo_1 criado")

      r <- post("/pessoas", payload("fooo_2", "scala", "haskell"))
      _ <- verify(r._1 == Created && r._2 == "fooo_2 criado")

      r <- get("/pessoas/3BC0964C-9141-408D-A059-B85C1ACDE866")
      _ <- verify(r._1 == NotFound)

      r <- get("/pessoas?t=foo")
      _ <- verify(r._1 == Ok)

      r <- get("/contagem-pessoas")
      _ <- verify(r._1 == Ok)
      t = r._2.toInt

      futs =(1 to bulkSize).map { idx =>
              val p = payload(s"fooo_${idx + t}")
              post(s"/pessoas?idx=$idx", p)
            }
      rs <- Future.foldLeft(futs)(Seq.empty[Int])(_ :+ _._1)
      _  <- verify(rs.forall(_ == Created))
    } yield ()
  }

  val NotFound = 404
  val Created = 201
  val Ok = 200
  val BadRequest = 400

  def verify(check: => Boolean) =
    if (check) Future.unit
    else Future.failed(new RuntimeException("warm up failed"))

  def payload(apelido: String, stack: String*) = Some(
    s"""{
       |"apelido": "$apelido",
       |"nome": "QbJFZRFhbXmp",
       |"nascimento": "1949-07-25"
       |${
          stack match {
            case Nil => ""
            case _   => s""" ,"stack": [ ${stack.map(q).mkString(" ,")} ]"""
          }
        }
       |}""".stripMargin)

  def q(s: String) = s""""$s""""

}

// Future based...
trait WarmUpProtocol {
  type Response = (Int, String)

  def get(url: String): Future[Response]

  def post(url: String, body: Option[String]): Future[Response]
}