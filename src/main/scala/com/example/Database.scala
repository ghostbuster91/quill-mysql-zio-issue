package com.example

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.logging.{Logger, Logging, log}
import zio.macros.accessible
import zio.random.Random
import zio.{Has, RIO, RManaged, Schedule, Task, URLayer, ZIO, ZManaged}

import java.sql.{Connection, SQLException}

@accessible
trait Database {
  def acquireConnection(): ZManaged[Any, SQLException, Connection]

  def migrate(): Task[Unit]

  def clean(): Task[Unit]
}

object Database {
  type DatabaseEnv = Has[DbConfig] with Logging with Random with Clock

  private val makeHikariConfig: RIO[DatabaseEnv, HikariConfig] =
    for {
      _ <- log.info("Loading Hikari configuration")
      conf <- ZIO.service[DbConfig]
      _ <- log.info(s"The database jdbc url is ${conf.jdbcUrl}")
      hikariConfig <- ZIO.effect {
        val hConfig = new HikariConfig()
        hConfig.setUsername(conf.user)
        hConfig.setPassword(conf.password)
        hConfig.setJdbcUrl(conf.jdbcUrl)
        hConfig
      }
    } yield hikariConfig

  private def makeHikariDataSourceManaged(hikariConfig: HikariConfig): RManaged[DatabaseEnv, HikariDataSource] = {
    ZManaged.fromAutoCloseable(Blocking.Service.live.blocking(ZIO.effect(new HikariDataSource(hikariConfig))))
  }

  private val makeServiceManaged: RManaged[DatabaseEnv, Database] = {
    log.info("Creating Database service").toManaged_ *>
      makeHikariConfig.toManaged_ >>=
      makeHikariDataSourceManaged >>= { datasource =>
      ZManaged.service[Logger[String]] >>= { logger =>
        ZManaged.service[Clock.Service].map { clock =>
          createDb(datasource, clock, logger)
        }
      }
    }
  }

  private def createDb(datasource: HikariDataSource, clock: Clock.Service, logger: Logger[String]): Database = {
    new Database {
      val acquireConnection: ZManaged[Any, SQLException, Connection] =
        ZManaged
          .fromAutoCloseable(
            ZIO.effect(datasource.getConnection).refineToOrDie[SQLException]
          )

      private val flyway = Flyway
        .configure()
        .dataSource(datasource)
        .schemas("test")
        .lockRetryCount(10)
        .load()


      val migrate: Task[Unit] =
        for {
          _ <- Blocking.Service.live
            .effectBlocking(flyway.migrate())
            .timeoutFail(new RuntimeException("timeout"))(10 seconds)
            .provide(Has(clock))
            .tapError(logger.throwable("Could not complete flyway migration", _))
        } yield ()

      val clean: Task[Unit] = Blocking.Service.live.effectBlocking(flyway.clean())
    }
  }

  val live: URLayer[DatabaseEnv, Has[Database]] =
    makeServiceManaged.toLayer
      .tapError(log.throwable("Could not access Database. Eventually retrying", _))
      .retry(Schedule.exponential(50 millis).jittered && Schedule.recurs(10))
      .tapError(log.throwable("Could not access Database after 10 times, dying.", _))
      .orDie
}
