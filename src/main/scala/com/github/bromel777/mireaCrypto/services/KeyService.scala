package com.github.bromel777.mireaCrypto.services

import java.security.PrivateKey

trait KeyService[F[_]] {
  def createKey: F[PrivateKey]
}
