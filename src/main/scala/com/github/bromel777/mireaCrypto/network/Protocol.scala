package com.github.bromel777.mireaCrypto.network

import scodec._
import scodec.bits._
import codecs._
import scodec.codecs.implicits._

object Protocol {

  sealed trait UserMessage
  object UserMessage {
    case class RegisterKey(publicKeyBytes: BitVector, signatureBytes: BitVector) extends UserMessage
    case class SendMsgToUser(recipientPublicKeyBytes: BitVector, msgCyptherBytes: BitVector) extends UserMessage

    val codec: Codec[UserMessage] = discriminated[UserMessage]
      .by(uint8)
      .typecase(1, (Codec[BitVector] :: Codec[BitVector]).as[RegisterKey])
      .typecase(2, (Codec[BitVector] :: Codec[BitVector]).as[SendMsgToUser])
  }
}
