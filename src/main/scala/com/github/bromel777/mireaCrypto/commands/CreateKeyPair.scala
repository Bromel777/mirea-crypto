package com.github.bromel777.mireaCrypto.commands

import cats.Monad
import com.github.bromel777.mireaCrypto.services.KeyService
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import tofu.common.Console
import tofu.syntax.console._

case class CreateKeyPair[F[_]: Monad: Logger: Console](keyService: KeyService[F]) extends Command[F] {

  override val name: String = "createKeyPair"

  override def execute: F[Unit] = for {
    keyPair <- keyService.createKeyPair
    _ <- keyService.saveKeyPair(keyPair)
    _ <- Logger[F].info(s"Create key pair with public key: ${keyPair.getPublic}")
    _ <- putStrLn(s"Wow! You've create keypair with public key ${keyPair.getPublic}. Now register it!")
  } yield ()
}
