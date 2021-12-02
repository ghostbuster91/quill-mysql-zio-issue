package com.example

import io.getquill._

object QuillSupport {

  val context: MysqlZioJdbcContext[CompositeNamingStrategy2[MysqlEscape.type, SnakeCase.type]] =
    new MysqlZioJdbcContext(NamingStrategy(MysqlEscape, SnakeCase))

  import context._

  trait Schemas {
    implicit val personSchema = schemaMeta[Person]("`person`")
  }
}
