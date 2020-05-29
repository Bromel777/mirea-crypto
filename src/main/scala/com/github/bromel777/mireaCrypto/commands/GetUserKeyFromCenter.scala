package com.github.bromel777.mireaCrypto.commands

import java.net.InetSocketAddress

import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.{GetUserKey, RegisterKey}
import com.github.bromel777.mireaCrypto.services.KeyService
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import fs2.concurrent.Queue
import scodec.bits.BitVector

case class GetUserKeyFromCenter[F[_]](toNetMsgsQueue: Queue[F, (UserMessage, InetSocketAddress)],
                                      keyService: KeyService[F],
                                      settings: ApplicationSettings) extends Command[F] {

  override val name: String = "getUserKey"

  override def execute(args: List[String]): F[Unit] = toNetMsgsQueue.enqueue1(
    GetUserKey(BitVector(args.head.getBytes)) -> settings.certificationCenterAddr.toInetSocketAddress
    )
}
