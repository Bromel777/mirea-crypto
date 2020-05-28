package com.github.bromel777.mireaCrypto.programs

import cats.Applicative
import cats.effect.Sync
import fs2.Stream
import cats.implicits._
import com.github.bromel777.mireaCrypto.commands.Command
import com.github.bromel777.mireaCrypto.levelDb.Database
import com.github.bromel777.mireaCrypto.services.KeyService
import io.chrisdavenport.log4cats.Logger
import tofu.common.Console
import tofu.syntax.console._

trait ConsoleProgram[F[_]] {

  def run: Stream[F, Unit]
}

object ConsoleProgram {
  private class Live[F[_]: Sync: Logger: Console](keyService: KeyService[F]) extends ConsoleProgram[F] {

    private val commands = Command.commands(keyService)

    private val readCommand: F[Unit] = for {
      _ <- putStrLn("Write your command:")
      command <- readStrLn
      _ <- commands.find(_.name == command).get.execute
      _ <- putStrLn(s"Your command: $command")
    } yield ()

    override def run: Stream[F, Unit] = Stream(())
      .covary[F]
      .repeat
      .evalMap[F, Unit](_ => readCommand)
  }

  def apply[F[_]: Sync: Console: Logger](keyService: KeyService[F]): F[ConsoleProgram[F]] =
    Applicative[F].pure(new Live[F](keyService))
}
