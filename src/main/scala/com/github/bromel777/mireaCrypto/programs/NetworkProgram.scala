package com.github.bromel777.mireaCrypto.programs

import cats.effect.{Concurrent, ContextShift, Sync}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.Logger
import cats.syntax.applicative._

trait NetworkProgram[F[_]] {

  def run: Stream[F, Unit]
}

object NetworkProgram {

  private final class Live[F[_]: Concurrent: ContextShift: Logger](socketGroup: SocketGroup) extends NetworkProgram[F] {

    val startServer =
      Stream.eval(Logger[F].info("Start server at port 3434!"))

    override def run: Stream[F, Unit] = startServer
  }

  def apply[F[_]: Concurrent: ContextShift: Logger](socketGroup: SocketGroup): F[NetworkProgram[F]] =
    Sync[F].delay(new Live(socketGroup))
}
