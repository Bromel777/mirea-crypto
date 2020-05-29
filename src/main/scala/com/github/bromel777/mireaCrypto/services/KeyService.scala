package com.github.bromel777.mireaCrypto.services

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyPair, KeyPairGenerator, PrivateKey, PublicKey, SecureRandom}

import cats.Applicative
import cats.effect.Sync
import com.github.bromel777.mireaCrypto.levelDb.Database
import io.chrisdavenport.log4cats.Logger
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import cats.implicits._

trait KeyService[F[_]] {
  def createKeyPair: F[KeyPair]
  def saveKeyPair(pair: KeyPair): F[Unit]
  def getKeyPair: F[(PrivateKey, PublicKey)]
}

object KeyService {

  private val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
  private val g: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC")
  val fact: KeyFactory = KeyFactory.getInstance("ECDSA", "BC");

  g.initialize(ecSpec, new SecureRandom())

  private val privateKeyDbKey = "privateKey".getBytes()
  private val publicKeyDbKey = "publicKey".getBytes()

  private class Live[F[_]: Sync: Logger](db: Database[F]) extends KeyService[F] {

    override def createKeyPair: F[KeyPair] = Sync[F].delay(g.generateKeyPair)

    override def saveKeyPair(pair: KeyPair): F[Unit] = for {
      _ <- db.put(privateKeyDbKey, pair.getPrivate.getEncoded)
      _ <- db.put(publicKeyDbKey, pair.getPublic.getEncoded)
    } yield ()

    override def getKeyPair: F[(PrivateKey, PublicKey)] = for {
      publicKeyBytes <- db.get(publicKeyDbKey)
      privateKeyBytes <- db.get(privateKeyDbKey)
      publicKey <- Sync[F].delay(fact.generatePublic(new X509EncodedKeySpec(publicKeyBytes.get)))
      privateKey <- Sync[F].delay(fact.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes.get)))
    } yield (privateKey, publicKey)
  }

  def apply[F[_]: Sync: Logger](db: Database[F]): F[KeyService[F]] = Applicative[F].pure(new Live(db))
}
