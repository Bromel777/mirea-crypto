package com.github.bromel777.mireaCrypto.utils

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object Blowfish {

  def encrypt(input: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val secretKeySpec = new SecretKeySpec(key, "Blowfish")
    val cipher = Cipher.getInstance("Blowfish")
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
    cipher.doFinal(input)
  }

  def decrypt(input: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val secretKeySpec = new SecretKeySpec(key, "Blowfish")
    val cipher = Cipher.getInstance("Blowfish")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
    cipher.doFinal(input)
  }
}
