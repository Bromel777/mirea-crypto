package com.github.bromel777.mireaCrypto.network

import scodec._
import scodec.bits._
import codecs._
import scodec.codecs.implicits._

object Protocol {

  sealed trait UserMessage
  object UserMessage {
    case class RegisterKey(publicKeyBytes: BitVector, signatureBytes: BitVector, userLogin: BitVector) extends UserMessage
    case class SendMsgToUser(recipientPublicKeyBytes: BitVector, msgCyptherBytes: BitVector) extends UserMessage
    case class GetUserKey(userLogin: BitVector) extends UserMessage
    case class UserPublicKey(publicKeyBytes: BitVector, userLogin: BitVector) extends UserMessage
    case class InitDialog(key: BitVector) extends UserMessage

    val codec: Codec[UserMessage] = discriminated[UserMessage]
      .by(uint8)
      .typecase(1, (Codec[BitVector] :: Codec[BitVector] :: Codec[BitVector]).as[RegisterKey])
      .typecase(2, (Codec[BitVector] :: Codec[BitVector]).as[SendMsgToUser])
      .typecase(3, Codec[BitVector].as[GetUserKey])
      .typecase(4, (Codec[BitVector] :: Codec[BitVector]).as[UserPublicKey])
      .typecase(5, Codec[BitVector].as[InitDialog])
  }
}
