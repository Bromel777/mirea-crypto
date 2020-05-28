package com.github.bromel777.mireaCrypto.commands

import cats.Monad
import cats.effect.Sync
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.services.KeyService
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger
import tofu.common.Console

trait Command[F[_]] {

  val name: String

  def execute(args: List[String] = List.empty): F[Unit]
}

object Command {

  def commands[F[_]: Sync: Logger: Console](keyService: KeyService[F],
                                            toNetMsgs: Queue[F, UserMessage]): List[Command[F]] = List(
    CreateKeyPair[F](keyService),
    SendMsg[F](toNetMsgs)
  )
}
