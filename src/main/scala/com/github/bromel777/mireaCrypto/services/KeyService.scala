package com.github.bromel777.mireaCrypto.services

import java.security.{KeyPair, PrivateKey}

trait KeyService[F[_]] {
  def createKeyPair: F[KeyPair]
  def saveKeyPair(pair: KeyPair): F[Unit]
}
