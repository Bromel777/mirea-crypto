package com.github.bromel777.mireaCrypto.commands

import java.security.{KeyPairGenerator, SecureRandom}

import cats.effect.Sync
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import fs2.concurrent.Queue
import tofu.common.Console
import tofu.syntax.console._
import cats.implicits._
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.SendMsgToUser
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import scodec.bits.BitVector

case class SendMsg[F[_]: Sync: Console](toNetMsgsQueue: Queue[F, UserMessage]) extends Command[F] {

  private val ecSpec: ECNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec("prime192v1")
  private val g: KeyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC")

  g.initialize(ecSpec, new SecureRandom())

  val pair = g.generateKeyPair()

  override val name: String = "send"

  override def execute(args: List[String]): F[Unit] = for {
    _ <- putStrLn(s"Write msg: ${args.head}")
    _ <- toNetMsgsQueue.enqueue1(SendMsgToUser(BitVector(pair.getPublic.getEncoded), BitVector(args.head.getBytes())))
  } yield ()
}
