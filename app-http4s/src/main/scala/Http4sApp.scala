package etc.rinha.app

import cats.effect._
import cats.effect.unsafe.IORuntime
import com.comcast.ip4s._
import etc.rinha.app.service.{ PessoaService, byPass, inDatabase, inMemory }
import etc.rinha.shared.PessoaOut
import java.util.UUID
import org.http4s.HttpApp
import org.http4s.ember.server._
import org.postgresql.ds.{ PGPoolingDataSource, PGSimpleDataSource }

object Http4sApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val cfg = AppConfig()
    val host = ipv4"0.0.0.0"
    val port = Port.fromInt(cfg.appPort).get // let it crash
    val initialHeath = false

    implicit val _io = runtime

    for {
      _      <- IO.println(s"....server starting at $host:$port (health: $initialHeath)")
      health <- IO.ref(initialHeath)
      svc    <- makeService(cfg)
      app    = AppRoutes.api(health, svc)

      _     <- warmup(app, svc, cfg.warmUpBulkSize, health)

      _     <- EmberServerBuilder
                .default[IO]
                .withHost(host)
                .withPort(port)
                .withHttpApp(app)
                .build
                .useForever

    } yield ExitCode.Success
  }

  // private

  private def warmup(
    app: HttpApp[IO],
    svc: PessoaService[IO],
    bulk: Int = 128,
    href: Ref[IO, Boolean]
  )(implicit io: IORuntime): IO[Unit] =
    for {
      _ <- IO.println("... starting warm up")
      t <- (
              svc.deleteApelidoLike("fooo_") *>
              WarmUp.service(app, bulk) *>
              svc.deleteApelidoLike("fooo_")
          ).timed
      _ <- href.update(_ => true)
      _ <- IO.println(s"... server is ready (wup took ${t._1.toMillis} millis)!")
    } yield ()

  private def makeService(cfg: AppConfig): IO[PessoaService[IO]] =
    if (cfg.impl == MEM)
        IO.println("... Using in memory service...") >>
        IO.ref(Map[UUID, PessoaOut]()).map(inMemory[IO](_))
    else if (cfg.impl == PASS)
      IO.println("... Using 'bypass' service...") >>
      IO.pure(byPass[IO])
    else
      IO.println("... using real service...") >>
      IO.delay(inDatabase[IO](cfg))

  trait ServiceImpl

  case object MEM extends ServiceImpl
  case object PASS extends ServiceImpl
  case object DB extends ServiceImpl

  object ServiceImpl {
    def apply(i: String): ServiceImpl =
      if (i.toLowerCase.contains("memory"))  MEM
      else if (i.toLowerCase.contains("pass")) PASS
      else DB
  }

  case class AppConfig(
    appPort: Int,
    dbHost: String,
    impl: ServiceImpl,
    warmUpBulkSize: Int
  ) {
    def newDatasource: javax.sql.DataSource = {
      val ds = new PGSimpleDataSource()
      ds.setUser("brawler")
      ds.setPassword("brawlerp")
      ds.setPortNumbers(Array(5432))
      ds.setServerNames(Array(dbHost))
      ds
    }
  }

  object AppConfig {
    def apply(): AppConfig =  AppConfig(
      appPort = sys.env.get("APP_PORT").map(_.toInt).getOrElse(9999),
      dbHost = sys.env.get("DB_HOST").getOrElse("localhost"),
      impl = sys.env.get("APP_SVC_IMPL").map(ServiceImpl(_)).getOrElse(DB),
      warmUpBulkSize = sys.env.get("APP_BULK_SIZE").map(_.toInt).getOrElse(2048)
    )
  }

}