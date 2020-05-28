package com.github.bromel777.mireaCrypto

import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.RegisterKey
import com.github.bromel777.mireaCrypto.utils.ECDSA
import scodec.bits.BitVector

object Application extends App {

  println(ECDSA.privateKey)
  val publicKey = ECDSA.public
  val userMessage = RegisterKey(publicKey, BitVector(Array((1: Byte))))
  println(UserMessage.codec.encode(userMessage))
}
