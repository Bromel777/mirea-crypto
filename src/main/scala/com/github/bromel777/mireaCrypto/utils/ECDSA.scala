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
  val public: PublicKey = fact.generatePublic(new X509EncodedKeySpec(pair.getPublic.getEncoded));
  val privateKey: PrivateKey = fact.generatePrivate(new PKCS8EncodedKeySpec(pair.getPrivate.getEncoded));

  val firstPair = g.genKeyPair()
  val firstPublic = firstPair.getPublic.asInstanceOf[BCECPublicKey]
  val firstPrivate = firstPair.getPrivate.asInstanceOf[BCECPrivateKey]
  val secondPair = g.genKeyPair()
  val secondPrivate = secondPair.getPrivate.asInstanceOf[BCECPrivateKey]
  val secondPublic = secondPair.getPublic.asInstanceOf[BCECPublicKey]
  val pubKey = pair.getPublic
  val privKey = pair.getPrivate.asInstanceOf[BCECPrivateKey]
//  val pubKeyParams = new ECPublicKeyParameters(parameters.getG.multiply(privKey.getD), parameters)
//  println(s"At 123: ${parameters.getG.multiply(new BigInteger(Array(123: Byte)))}")
//  println("All sum: " + parameters.getG.multiply(new BigInteger(Array(123: Byte))).add(pubKey))
//
//  println(s"Private key: ${privKey.getD}")
//  val res = multiplier.multiply(generator, privKey.getD)
//  println(s"prx = ${res.getXCoord}")
//  println(s"pry = ${res.getYCoord}")
//  val res = pubKey.getQ.multiply(new BigInteger(Array(123: Byte)))
//  println(res)
 // val anotherRes = res.multiply()
//  println("=================")
}
