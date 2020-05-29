package com.github.bromel777.mireaCrypto.programs

import java.net.InetSocketAddress

import cats.Applicative
import cats.effect.Sync
import fs2.Stream
import cats.implicits._
import com.github.bromel777.mireaCrypto.commands.Command
import com.github.bromel777.mireaCrypto.levelDb.Database
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.services.KeyService
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import tofu.common.Console
import tofu.syntax.console._

trait ConsoleProgram[F[_]] {

  def run: Stream[F, Unit]
}

object ConsoleProgram {

  private class Live[F[_]: Sync: Logger: Console](keyService: KeyService[F],
                                                  netOutMsgsQueue: Queue[F, (UserMessage, InetSocketAddress)],
                                                  settings: ApplicationSettings) extends ConsoleProgram[F] {

    private val commands = Command.commands(keyService, netOutMsgsQueue, settings)

    private val readCommand: F[Unit] = for {
      _ <- putStrLn("Write your command:")
      input <- readStrLn
      _ <- {
        input.split(" ").toList match {
          case command :: args => commands.find(_.name == command).get.execute(args)
        }
      }
    } yield ()

    override def run: Stream[F, Unit] = Stream(())
      .covary[F]
      .repeat
      .evalMap[F, Unit](_ => readCommand)
  }

  def apply[F[_]: Sync: Console: Logger](keyService: KeyService[F],
                                         netOutMsgsQueue: Queue[F, (UserMessage, InetSocketAddress)],
                                         settings: ApplicationSettings): F[ConsoleProgram[F]] =
    Applicative[F].pure(new Live[F](keyService, netOutMsgsQueue, settings))
}
