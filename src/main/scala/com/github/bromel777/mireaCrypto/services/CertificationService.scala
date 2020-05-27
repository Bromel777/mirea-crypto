package com.github.bromel777.mireaCrypto.services

import java.security.PrivateKey

import com.comcast.ip4s.{Ipv4Address, SocketAddress}

trait CertificationService[F[_]] {
  def registerKeyPair(privateKey: PrivateKey, certCenterIp: SocketAddress[Ipv4Address]): F[Boolean]
}
