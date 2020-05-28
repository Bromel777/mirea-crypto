package com.github.bromel777.mireaCrypto.commands

import cats.Monad
import com.github.bromel777.mireaCrypto.services.KeyService
import io.chrisdavenport.log4cats.Logger
import tofu.common.Console

trait Command[F[_]] {

  val name: String

  def execute: F[Unit]
}

object Command {

  def commands[F[_]: Monad: Logger: Console](keyService: KeyService[F]): List[Command[F]] = List(
    CreateKeyPair[F](keyService)
  )
}
