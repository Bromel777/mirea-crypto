package com.github.bromel777.mireaCrypto.programs

import java.net.InetSocketAddress
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.net.{SocketAddress => JSocketAddr}

import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import cats.syntax.applicative._
import com.comcast.ip4s.{Ipv4Address, SocketAddress}
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.{RegisterKey, SendMsgToUser}
import com.github.bromel777.mireaCrypto.services.{CertificationService, KeyService, SocketService}
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import fs2.concurrent.Queue

trait NetworkProgram[F[_]] {

  def run: Stream[F, Unit]
}

object NetworkProgram {

  private final class Live[F[_]: Concurrent: ContextShift: Logger]
  (socketGroup: SocketGroup,
   toConnectQueue: Queue[F, SocketAddress[Ipv4Address]],
   toNetMsgsQueue: Queue[F, UserMessage],
   config: ApplicationSettings,
   certCervice: CertificationService[F]
  ) extends NetworkProgram[F] {

    private val startServer = (for {
      _           <- Stream.eval(Logger[F].info(s"Start server at port ${config.bindPort}"))
      socketRes   <- socketGroup.server(new InetSocketAddress(config.bindPort.value))
      _           <- Stream.eval(Logger[F].info(s"Got: ${socketRes}"))
      socket      <- Stream.resource(socketRes)
      service     <- Stream.resource(SocketService(socket, toNetMsgsQueue))
    } yield service.read).parJoinUnbounded.evalMap { case (msg, remote) =>
      for {
        _ <- Logger[F].info(s"Got ${msg}")
        _ <- proccessIncomingMsg(
          msg,
          remote,
          KeyService.fact
        )
      } yield ()
    }

    private val simpleConnect: Stream[F, Unit] = (for {
      peer <- toConnectQueue.dequeue
      _ <- Stream.eval(Logger[F].info(s"Trying to connect with $peer"))
      service <- Stream.resource(SocketService(socketGroup, peer, toNetMsgsQueue))
    } yield service.read).parJoinUnbounded.evalMap { case (msg, remote) =>
      for {
        _ <- Logger[F].info(s"Got ${msg}")
        _ <- proccessIncomingMsg(
          msg,
          remote,
          KeyService.fact
        )
      } yield ()
    }

    private def proccessIncomingMsg(msg: UserMessage,
                                    clientIp: JSocketAddr,
                                    factory: KeyFactory): F[Unit] = msg match {
      case RegisterKey(publicKeyBytes, signatureBytes) => for {
        _ <- Logger[F].info(s"Register key for client ${clientIp}")
        _ <- certCervice.registerKeyPair(
          factory.generatePublic(new X509EncodedKeySpec(publicKeyBytes.toByteArray)),
          signatureBytes.toByteArray,
          clientIp.asInstanceOf[InetSocketAddress]
        )
      } yield ()
      case SendMsgToUser(myPublicKeyBytes, msgCyptherBytes) =>
        Logger[F].info(s"Receive msg: ${msgCyptherBytes.toByteArray.map(_.toChar).mkString}")
      case a => Logger[F].info(s"oh: ${a}")
    }

    override def run: Stream[F, Unit] = startServer concurrently simpleConnect
  }

  def apply[F[_]: Concurrent: ContextShift: Logger](config: ApplicationSettings,
                                                    toNetMsgsQueue: Queue[F, UserMessage],
                                                    certService: CertificationService[F]): Resource[F, NetworkProgram[F]] =
    Blocker[F].flatMap { blocker =>
      SocketGroup[F](blocker).evalMap { socketGroup =>
        for {
          queue <- Queue.bounded[F, SocketAddress[Ipv4Address]](100)
          _ <- config.knownPeers.traverse(peer => queue.enqueue1(peer))
        } yield new Live(socketGroup, queue, toNetMsgsQueue, config, certService)
      }
    }
}
