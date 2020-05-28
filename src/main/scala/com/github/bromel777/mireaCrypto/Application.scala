package com.github.bromel777.mireaCrypto

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, IO, IOApp, Sync}
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.RegisterKey
import com.github.bromel777.mireaCrypto.programs.{ConsoleProgram, NetworkProgram}
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import com.github.bromel777.mireaCrypto.utils.ECDSA
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import scodec.bits.BitVector
import fs2.Stream
import io.chrisdavenport.log4cats.Logger

final class Application[F[_]: Concurrent: ContextShift: Logger](config: ApplicationSettings) {

  def run: Stream[F, Unit] = programs.flatMap { case (netProg, consoleProg) =>
    netProg.run concurrently consoleProg.run
  }

  val programs = for {
    netProg <- Stream.resource(NetworkProgram[F](config))
    consoleProg <- Stream.eval(ConsoleProgram[F]())
  } yield (netProg, consoleProg)
}

object Application extends IOApp {

  println(ECDSA.privateKey)
  val publicKey = ECDSA.public
  val userMessage = RegisterKey(publicKey, BitVector(Array((1: Byte))))
  println(UserMessage.codec.encode(userMessage))

  def apply[F[_]: Concurrent: ContextShift](): F[Application[F]] = for {
    config  <- Sync[F].delay(ApplicationSettings.loadConfig("application.conf"))
    implicit0(logger: Logger[F])  <- Slf4jLogger.create[F]
  } yield new Application[F](config)

  override def run(args: List[String]): IO[ExitCode] =
    Application[IO]().flatMap(_.run.compile.drain.as(ExitCode.Success))
}
