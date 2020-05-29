package com.github.bromel777.mireaCrypto.commands

import cats.effect.Sync
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.services.KeyService
import fs2.concurrent.Queue
import cats.implicits._
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.RegisterKey
import scodec.bits.BitVector
import java.security.Signature


case class RegKey[F[_]: Sync](toNetMsgsQueue: Queue[F, UserMessage],
                              keyService: KeyService[F]) extends Command[F] {

  override val name: String = "registerKey"

  override def execute(args: List[String]): F[Unit] = for {
    keys <- keyService.getKeyPair
    signature <- Sync[F].delay {
      val ecdsaSign = Signature.getInstance("SHA1withECDSA")
      ecdsaSign.initSign(keys._1)
      ecdsaSign.update("Message".getBytes())
      ecdsaSign.sign()
    }
    _ <- toNetMsgsQueue.enqueue1(RegisterKey(BitVector(keys._2.getEncoded), BitVector(signature)))
  } yield ()
}
