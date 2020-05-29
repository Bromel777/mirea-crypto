package com.github.bromel777.mireaCrypto.utils

import java.math.BigInteger
import java.security.interfaces.ECPublicKey
import java.security.{KeyFactory, KeyPair, KeyPairGenerator, PrivateKey, PublicKey, SecureRandom, Security}
import java.security.spec.{ECPoint, PKCS8EncodedKeySpec, X509EncodedKeySpec}

import org.bouncycastle.crypto.params.{ECDomainParameters, ECPublicKeyParameters}
import org.bouncycastle.jcajce.provider.asymmetric.ec.{BCECPrivateKey, BCECPublicKey}
import org.bouncycastle.jce.{ECNamedCurveTable, ECPointUtil}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec

object ECDSA {

  Security.addProvider(new BouncyCastleProvider)

  val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
  val parameters = new ECDomainParameters(ecSpec.getCurve, ecSpec.getG, ecSpec.getH, ecSpec.getN)
  val g: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC")
  g.initialize(ecSpec, new SecureRandom())
  val pair: KeyPair = g.genKeyPair()

  val fact: KeyFactory = KeyFactory.getInstance("ECDSA", "BC");
}
