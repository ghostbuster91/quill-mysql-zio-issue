package com.example

import com.example.QuillSupport.context._
import com.example.QuillSupport.{Schemas, context}
import io.getquill.context.ZioJdbc.QIO
import zio.{Has, ULayer, ZLayer}

trait PersonRepository {
  def insertPersonBatch(batch: List[Person]): QIO[Unit]
}

object PersonRepository {
  val live: ULayer[Has[PersonRepository]] = ZLayer.succeed(new Live)

  class Live extends PersonRepository with Schemas {
    override def insertPersonBatch(batch: List[Person]): QIO[Unit] = {
      context.run(liftQuery(batch).foreach(query[Person].insert(_))).unit
    }
  }
}
