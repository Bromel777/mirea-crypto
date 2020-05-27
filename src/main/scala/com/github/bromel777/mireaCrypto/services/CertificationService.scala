package com.github.bromel777.mireaCrypto.services

trait CertificationService[F[_]] {

  def createKeyPair: F[]
}
