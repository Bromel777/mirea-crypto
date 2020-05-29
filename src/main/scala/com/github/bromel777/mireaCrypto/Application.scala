package com.github.bromel777.mireaCrypto

import java.io.File

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, IO, IOApp, Sync}
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.RegisterKey
import com.github.bromel777.mireaCrypto.programs.{ConsoleProgram, NetworkProgram}
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import com.github.bromel777.mireaCrypto.utils.ECDSA
import cats.implicits._
import com.github.bromel777.mireaCrypto.levelDb.Database
import com.github.bromel777.mireaCrypto.services.{CertificationService, KeyService}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import scodec.bits.BitVector
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger

final class Application[F[_]: Concurrent: ContextShift: Logger](config: ApplicationSettings) {

  def run: Stream[F, Unit] = programs.flatMap { case (netProg, consoleProg) =>
    netProg.run concurrently consoleProg.run
  }

  private val database = for {
    db <- Database[F](new File(config.dbFolder))
  } yield db

  private def services(db: Database[F]) = for {
    keyService <- KeyService[F](db)
    certService <- CertificationService[F](db)
  } yield (keyService, certService)

  private val programs = for {
    db <- Stream.resource(database)
    services <- Stream.eval(services(db))
    queue <- Stream.eval(Queue.bounded[F, UserMessage](100))
    netProg <- Stream.resource(NetworkProgram[F](config, queue, services._2))
    consoleProg <- Stream.eval(ConsoleProgram[F](services._1, queue))
  } yield (netProg, consoleProg)
}

object Application extends IOApp {

  println(ECDSA.privateKey)
  val publicKey = ECDSA.public
  val userMessage = RegisterKey(BitVector(publicKey.getEncoded), BitVector(Array((1: Byte))))
  println(publicKey.getEncoded.length)
  val encoded = UserMessage.codec.encode(userMessage).require
  println(encoded)
  println(UserMessage.codec.decode(encoded).require)

  def apply[F[_]: Concurrent: ContextShift](): F[Application[F]] = for {
    config  <- Sync[F].delay(ApplicationSettings.loadConfig("application.conf"))
    implicit0(logger: Logger[F])  <- Slf4jLogger.create[F]
  } yield new Application[F](config)

  override def run(args: List[String]): IO[ExitCode] =
    Application[IO]().flatMap(_.run.compile.drain.as(ExitCode.Success))
}
