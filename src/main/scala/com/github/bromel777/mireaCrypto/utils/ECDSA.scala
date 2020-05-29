package com.github.bromel777.mireaCrypto.utils

import java.security.{KeyFactory, KeyPair, KeyPairGenerator, PrivateKey, PublicKey, SecureRandom, Security}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec

object ECDSA {

  Security.addProvider(new BouncyCastleProvider)

  val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
  val g: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC")
  g.initialize(ecSpec, new SecureRandom())
  val pair: KeyPair = g.generateKeyPair

  val fact: KeyFactory = KeyFactory.getInstance("ECDSA", "BC");
  val public: PublicKey = fact.generatePublic(new X509EncodedKeySpec(pair.getPublic.getEncoded));
  val privateKey: PrivateKey = fact.generatePrivate(new PKCS8EncodedKeySpec(pair.getPrivate.getEncoded));
}
