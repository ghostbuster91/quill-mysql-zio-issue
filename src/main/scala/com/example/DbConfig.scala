package com.example

case class DbConfig(
                      host: String,
                      port: Int,
                      user: String,
                      password: String,
                      name: String,
                      poolSize: Int = 5
                   ) {

  val jdbcUrl: String = s"jdbc:mysql://$host:$port/$name"
}
