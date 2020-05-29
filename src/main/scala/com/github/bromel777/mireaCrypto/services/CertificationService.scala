package com.github.bromel777.mireaCrypto.services

import java.net.InetSocketAddress
import java.security.spec.X509EncodedKeySpec
import java.security.{PrivateKey, PublicKey, Signature}

import cats.Applicative
import cats.effect.Sync
import com.comcast.ip4s.{Ipv4Address, SocketAddress}
import com.github.bromel777.mireaCrypto.levelDb.Database
import io.chrisdavenport.log4cats.Logger
import cats.implicits._
import com.github.bromel777.mireaCrypto.services.KeyService.fact

trait CertificationService[F[_]] {
  def registerKeyPair(publicKey: PublicKey,
                      signature: Array[Byte],
                      userLogin: Array[Byte]): F[Boolean]

  def saveUserKey(publicKey: Array[Byte], userLogin: Array[Byte]): F[Unit]

  def getUserPublicKey(userLogin: Array[Byte]): F[PublicKey]
}

object CertificationService {

  private class Live[F[_]: Sync: Logger](db: Database[F]) extends CertificationService[F] {

    override def registerKeyPair(publicKey: PublicKey,
                                 signature: Array[Byte],
                                 userLogin: Array[Byte]): F[Boolean] = for {
      res <- Sync[F].delay {
        val ecdsaVerify = Signature.getInstance("SHA1withECDSA")
        ecdsaVerify.initVerify(publicKey)
        ecdsaVerify.update("Message".getBytes)
        ecdsaVerify.verify(signature)
      }
      _ <- Logger[F].info(s"Verification result ${res} for user: ${userLogin.map(_.toChar).mkString}")
      _ <- saveUserKey(publicKey.getEncoded, userLogin)
    } yield res

    override def getUserPublicKey(userLogin: Array[Byte]): F[PublicKey] = for {
      possibleBytes <- db.get(s"${userLogin.map(_.toChar).mkString} pubKey".getBytes)
    } yield (KeyService.fact.generatePublic(new X509EncodedKeySpec(possibleBytes.get)))

    override def saveUserKey(publicKey: Array[Byte], userLogin: Array[Byte]): F[Unit] =
      db.put(s"${userLogin.map(_.toChar).mkString} pubKey".getBytes, publicKey)
  }

  def apply[F[_]: Sync: Logger](db: Database[F]): F[CertificationService[F]] =
    Applicative[F].pure(new Live[F](db))
}
