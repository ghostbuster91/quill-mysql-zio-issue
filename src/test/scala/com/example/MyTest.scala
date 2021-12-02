package com.example

import zio.logging.slf4j.Slf4jLogger
import zio.logging.{LogAnnotation, Logging}
import zio.test.Assertion.anything
import zio.test._
import zio.{Has, ULayer, ZIO, ZLayer}

import java.sql.SQLException

object MyTest extends DefaultRunnableSpec {
  val logger: ULayer[Logging] =
    Slf4jLogger.makeWithAnnotationsAsMdc(List(LogAnnotation.CorrelationId))

  private val value: ZLayer[Any, Nothing, zio.ZEnv with Logging with Has[Database] with Annotations] = environment.liveEnvironment ++ logger ++ ZLayer.succeed(DbConfig("127.0.0.1", 3306, "root", "qwe", "test")) >+> (Database.live ++ Annotations.live)


  val spec: ZSpec[Any, Any] = (suite("PersonRepository")(
    testM("should not allow to insert two people with the same id") {
      val repository = new PersonRepository.Live

      val batchInsert = Database.acquireConnection().use { conn =>
        QuillSupport.context.transaction(repository.insertPersonBatch(List(Person("a", "a"), Person("a", "a"))))

          .provide(Has(conn))
      }
      assertM(batchInsert.run)(Assertion.fails(Assertion.isSubtype[SQLException](anything)))


    }) @@ IntegrationTests.cleanStateSeq @@ IntegrationTests.nonFlaky(100))
    .provideLayerShared(value)
}
