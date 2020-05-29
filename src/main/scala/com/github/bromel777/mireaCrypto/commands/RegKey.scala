package com.github.bromel777.mireaCrypto.commands

import java.net.InetSocketAddress

import cats.effect.Sync
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.services.KeyService
import fs2.concurrent.Queue
import cats.implicits._
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.RegisterKey
import scodec.bits.BitVector
import java.security.Signature

import com.github.bromel777.mireaCrypto.settings.ApplicationSettings

case class RegKey[F[_]: Sync](toNetMsgsQueue: Queue[F, (UserMessage, InetSocketAddress)],
                              keyService: KeyService[F],
                              settings: ApplicationSettings) extends Command[F] {

  override val name: String = "registerKey"

  override def execute(args: List[String]): F[Unit] = for {
    keys <- keyService.getKeyPair
    signature <- Sync[F].delay {
      val ecdsaSign = Signature.getInstance("SHA1withECDSA")
      ecdsaSign.initSign(keys._1)
      ecdsaSign.update("Message".getBytes())
      ecdsaSign.sign()
    }
    _ <- toNetMsgsQueue.enqueue1(
      RegisterKey(
        BitVector(keys._2.getEncoded),
        BitVector(signature),
        BitVector(settings.myLogin.getBytes())
      ) -> settings.certificationCenterAddr.toInetSocketAddress
    )
  } yield ()
}
