package com.github.bromel777.mireaCrypto.commands

import java.net.InetSocketAddress
import java.security.{KeyPairGenerator, SecureRandom}

import cats.effect.Sync
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import fs2.concurrent.Queue
import tofu.common.Console
import tofu.syntax.console._
import cats.implicits._
import com.comcast.ip4s.SocketAddress
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.SendMsgToUser
import com.github.bromel777.mireaCrypto.services.{CertificationService, CipherService}
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import com.github.bromel777.mireaCrypto.utils.Blowfish
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import scodec.bits.BitVector

case class SendMsg[F[_]: Sync: Console](toNetMsgsQueue: Queue[F, (UserMessage, InetSocketAddress)],
                                        cipherService: CipherService[F],
                                        settings: ApplicationSettings) extends Command[F] {

  override val name: String = "send"

  override def execute(args: List[String]): F[Unit] = for {
    _ <- putStrLn(s"Write msg: ${args.head}")
    code <- cipherService.deriveCipherKey(args.last)
    _ <- putStrLn(s"Cipher code: ${code.map(_.toChar).mkString}")
    _ <- toNetMsgsQueue.enqueue1(
      SendMsgToUser(BitVector(settings.myLogin.getBytes), BitVector(Blowfish.encrypt(args.head.getBytes, code))) ->
        SocketAddress.fromString4(args.init.last).get.toInetSocketAddress
    )
  } yield ()
}
