package com.github.bromel777.mireaCrypto.programs

import cats.Applicative
import cats.effect.Sync
import fs2.Stream
import cats.implicits._
import tofu.common.Console
import tofu.syntax.console._

trait ConsoleProgram[F[_]] {

  def run: Stream[F, Unit]
}

object ConsoleProgram {
  private class Live[F[_]: Sync: Console]() extends ConsoleProgram[F] {

    private val readCommand: F[Unit] = for {
      _ <- putStrLn("Write your command:")
      command <- readStrLn
      _ <- putStrLn(s"Your command: $command")
    } yield ()

    override def run: Stream[F, Unit] = Stream(())
      .covary[F]
      .repeat
      .evalMap[F, Unit](_ => readCommand)
  }

  def apply[F[_]: Sync: Console](): F[ConsoleProgram[F]] = Applicative[F].pure(new Live[F]())
}
