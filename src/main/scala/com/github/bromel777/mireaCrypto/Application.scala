package com.github.bromel777.mireaCrypto

import java.io.File
import java.net.InetSocketAddress
import java.security.Security

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, IO, IOApp, Sync}
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.{InitDialog, RegisterKey}
import com.github.bromel777.mireaCrypto.programs.{ConsoleProgram, NetworkProgram}
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import com.github.bromel777.mireaCrypto.utils.{Blowfish, ECDSA}
import cats.implicits._
import com.comcast.ip4s.SocketAddress
import com.github.bromel777.mireaCrypto.levelDb.Database
import com.github.bromel777.mireaCrypto.services.{CertificationService, CipherService, KeyService}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import scodec.bits.BitVector
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import org.bouncycastle.jce.provider.BouncyCastleProvider

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
    cipherService <- CipherService[F](certService, keyService)
  } yield (keyService, certService, cipherService)

  private val programs = for {
    db <- Stream.resource(database)
    services <- Stream.eval(services(db))
    queue <- Stream.eval(Queue.bounded[F, (UserMessage, InetSocketAddress)](100))
    netProg <- Stream.resource(NetworkProgram[F](config, queue, services._2, services._3))
    consoleProg <- Stream.eval(ConsoleProgram[F](services._1, queue, services._3, config))
  } yield (netProg, consoleProg)
}

object Application extends IOApp {

  Security.addProvider(new BouncyCastleProvider)

  val a = "test".getBytes
  val cipherText = Blowfish.encrypt(a, "123".getBytes)
  val decryptText = Blowfish.decrypt(cipherText, "123".getBytes)

  def apply[F[_]: Concurrent: ContextShift](): F[Application[F]] = for {
    config  <- Sync[F].delay(ApplicationSettings.loadConfig("application.conf"))
    implicit0(logger: Logger[F])  <- Slf4jLogger.create[F]
  } yield new Application[F](config)

  override def run(args: List[String]): IO[ExitCode] =
    Application[IO]().flatMap(_.run.compile.drain.as(ExitCode.Success))
}
