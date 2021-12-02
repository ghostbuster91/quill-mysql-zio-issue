package com.example

import zio.logging.{Logger, Logging}
import zio.prelude.{equalTo => _}
import zio.test.TestAspect.PerTest
import zio.test._
import zio.{Has, ZIO}

object IntegrationTests {

  val dbCleanupAfter: TestAspect[Nothing, Has[Database] with Logging, Throwable, Any] =
    TestAspect.after(
      ZIO.serviceWith[Logger[String]](_.info("cleaning database - start")) *> ZIO
        .service[Database]
        .flatMap(_.clean()) <* ZIO.serviceWith[Logger[String]](_.info("cleaning database - end"))
    )

  val dbMigrateBefore: TestAspect[Nothing, Has[Database] with Logging, Throwable, Any] =
    TestAspect.before(
      ZIO.serviceWith[Logger[String]](_.info("migration - start")) *> ZIO.service[Database].flatMap(_.migrate()).orElse {
        ZIO.serviceWith[Logger[String]](_.info("migration -  error, trying cleanup")) *> ZIO
          .service[Database]
          .flatMap(_.clean()) *> ZIO
          .service[Database]
          .flatMap(_.migrate())
      } <* ZIO.serviceWith[Logger[String]](_.info("migration - end"))
    )

  def nonFlaky(n: Int): TestAspectAtLeastR[Annotations] = {
    val nonFlaky = new PerTest.AtLeastR[Annotations] {
      def perTest[R <: Annotations, E](
                                        test: ZIO[R, TestFailure[E], TestSuccess]
                                      ): ZIO[R, TestFailure[E], TestSuccess] =
        test *> (test <* Annotations.annotate(TestAnnotation.repeated, 1)).repeatN(n - 1)
    }
    nonFlaky
  }

  val cleanStateSeq
  : TestAspect[Nothing, Has[Database] with Logging with Annotations, Throwable, Any] =
    IntegrationTests.dbMigrateBefore >>> IntegrationTests.dbCleanupAfter >>> TestAspect.sequential
}
