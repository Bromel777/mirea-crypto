package com.github.bromel777.mireaCrypto.services

import java.net.InetSocketAddress
import java.security.{PrivateKey, PublicKey, Signature}

import cats.Applicative
import cats.effect.Sync
import com.comcast.ip4s.{Ipv4Address, SocketAddress}
import com.github.bromel777.mireaCrypto.levelDb.Database
import io.chrisdavenport.log4cats.Logger
import cats.implicits._

trait CertificationService[F[_]] {
  def registerKeyPair(publicKey: PublicKey, signature: Array[Byte], userIp: InetSocketAddress): F[Boolean]
}

object CertificationService {

  private class Live[F[_]: Sync: Logger](db: Database[F]) extends CertificationService[F] {

    override def registerKeyPair(publicKey: PublicKey,
                                 signature: Array[Byte],
                                 userIp: InetSocketAddress): F[Boolean] = for {
      res <- Sync[F].delay {
        val ecdsaVerify = Signature.getInstance("SHA1withECDSA")
        ecdsaVerify.initVerify(publicKey)
        ecdsaVerify.update("Message".getBytes)
        ecdsaVerify.verify(signature)
      }
      _ <- Logger[F].info(s"Verification result: ${res}")
      _ <- db.put(s"$userIp pubKey".getBytes, publicKey.getEncoded)
    } yield (true)
  }

  def apply[F[_]: Sync: Logger](db: Database[F]): F[CertificationService[F]] =
    Applicative[F].pure(new Live[F](db))
}
