package etc.rinha.app

import cats.effect._
import com.comcast.ip4s._
import etc.rinha.Utils.retrier
import etc.rinha.app.service._
import etc.rinha.shared.PessoaOut
import java.util.UUID
import org.http4s.ember.server._
import org.postgresql.ds.PGSimpleDataSource

object App extends IOApp {

  // debug starvation 🤔 💭
  //override protected def blockedThreadDetectionEnabled = true

  override def run(args: List[String]): IO[ExitCode] = {
    val cfg = AppConfig()
    val host = ipv4"0.0.0.0"
    val port = Port.fromInt(cfg.appPort).get // let it crash
    val initialHeath = false

    for {
      _      <- IO.println(s"....server starting at $host:$port (health: $initialHeath)")
      health <- IO.ref(initialHeath)
      svc    <- makeService(cfg)
      app    = Routes.api(health, svc)

      _     <- WarmUp(svc, health, cfg.warmUpBulkSize)

      _     <- EmberServerBuilder
                .default[IO]
                .withHost(host)
                .withPort(port)
                .withHttpApp(app)
                .build
                .useForever

    } yield ExitCode.Success
  }

  private def makeService(cfg: AppConfig): IO[PessoaService[IO]] =
    if (cfg.impl == MEM)
        IO.println("... Using in memory service...") >>
        IO.ref(Map[UUID, PessoaOut]()).map(InMemory[IO](_))
    else if (cfg.impl == PASS)
      IO.println("... Using 'bypass' service...") >>
      IO.pure(ByPass[IO])
    else
      for {
          _ <- IO.println(s"... using real service [${cfg.dbHost}:5432/brawler]")
          c <- retrier(
            IO.blocking(cfg.newDatasource).map(_.getConnection),
            (t, s) => IO.println(s"error getting the connection: ${t.getMessage} (retrying in $s)")
          )
      } yield InDatabase[IO](c)

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
    // TODO: test w/ hikari
    def newDatasource: javax.sql.DataSource = {
      val ds = new PGSimpleDataSource()
      ds.setUser("brawler")
      ds.setPassword("brawler")
      ds.setDatabaseName("brawler")
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