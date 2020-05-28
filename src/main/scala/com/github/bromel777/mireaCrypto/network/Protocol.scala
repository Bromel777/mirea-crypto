package com.github.bromel777.mireaCrypto.network

import java.security.PublicKey

import scodec._
import scodec.bits._
import codecs._
import scodec.codecs.implicits._

object Protocol {

  val publicKeyCodec = Codec[BitVector].contramap((pubKey: PublicKey) => BitVector(pubKey.getEncoded))

  sealed trait UserMessage
  object UserMessage {
    case class RegisterKey(publicKey: PublicKey, signature: BitVector) extends UserMessage
    case class SendMsgToUser(recipientPublicKey: PublicKey, msgCyptherText: Array[Byte]) extends UserMessage

    val codec: Codec[UserMessage] = discriminated[UserMessage]
      .by(uint8)
      .typecase(1, (publicKeyCodec.encodeOnly :: Codec[BitVector]).as[RegisterKey])
      .typecase(2, (publicKeyCodec.encodeOnly :: Codec[BitVector]).as[.as[RegisterKey]])
  }
}
