package com.github.bromel777.mireaCrypto.services

import java.security.interfaces.ECPublicKey

import cats.Applicative
import cats.effect.Sync
import io.chrisdavenport.log4cats.Logger
import cats.implicits._
import org.bouncycastle.jcajce.provider.asymmetric.ec.{BCECPrivateKey, BCECPublicKey}

trait CipherService[F[_]] {

  def deriveCipherKey(userLogin: String): F[BigInt]
}

object CipherService {

  final class Live[F[_]: Sync: Logger](certificationService: CertificationService[F],
                                       keyService: KeyService[F]) extends CipherService[F] {

    override def deriveCipherKey(userLogin: String): F[BigInt] = for {
      userKey <- certificationService.getUserPublicKey(userLogin.getBytes)
      myKeys <- keyService.getKeyPair
      _ <- Logger[F].info(s"User public key: ${userKey}")
      _ <- Logger[F].info(s"MyPublicKey: ${myKeys._2}\n MyPrivateKey: ${myKeys._1}")
      cipherKey <- Sync[F].delay(
        userKey.asInstanceOf[BCECPublicKey].getQ.multiply(myKeys._1.asInstanceOf[BCECPrivateKey].getD)
      )
      _ <- Logger[F].info(s"Common key: ${BigInt(cipherKey.getEncoded(false).take(10))}")
    } yield BigInt(cipherKey.getEncoded(false).take(10))
  }

  def apply[F[_]: Sync: Logger](certificationService: CertificationService[F],
                                keyService: KeyService[F]): F[CipherService[F]] =
    Applicative[F].pure(new Live(certificationService, keyService))
}
