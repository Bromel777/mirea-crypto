package com.github.bromel777.mireaCrypto.services

import java.security.{KeyPair, KeyPairGenerator, PrivateKey, SecureRandom}

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
}

object KeyService {

  private val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
  private val g: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC")

  g.initialize(ecSpec, new SecureRandom())

  private val privateKeyDbKey = "privateKey".getBytes()
  private val publicKeyDbKey = "publicKey".getBytes()

  private class Live[F[_]: Sync: Logger](db: Database[F]) extends KeyService[F] {

    override def createKeyPair: F[KeyPair] = Sync[F].delay(g.generateKeyPair)

    override def saveKeyPair(pair: KeyPair): F[Unit] = for {
      _ <- db.put(privateKeyDbKey, pair.getPrivate.getEncoded)
      _ <- db.put(publicKeyDbKey, pair.getPublic.getEncoded)
    } yield ()
  }

  def apply[F[_]: Sync: Logger](db: Database[F]): F[KeyService[F]] = Applicative[F].pure(new Live(db))
}
