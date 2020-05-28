package com.github.bromel777.mireaCrypto

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.RegisterKey
import com.github.bromel777.mireaCrypto.programs.NetworkProgram
import com.github.bromel777.mireaCrypto.utils.ECDSA
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import scodec.bits.BitVector

object Application extends IOApp {

  println(ECDSA.privateKey)
  val publicKey = ECDSA.public
  val userMessage = RegisterKey(publicKey, BitVector(Array((1: Byte))))
  println(UserMessage.codec.encode(userMessage))

  override def run(args: List[String]): IO[ExitCode] = Blocker[IO]
    .use { blocker =>
      SocketGroup[IO](blocker).use { socketGroup =>
        Slf4jLogger.create[IO].flatMap { implicit logger =>
          NetworkProgram[IO](socketGroup).flatMap {
            _.run.compile.drain
          }
        }
      }
    }
    .as(ExitCode.Success)
}
