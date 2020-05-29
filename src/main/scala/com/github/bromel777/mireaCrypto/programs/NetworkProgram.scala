package com.github.bromel777.mireaCrypto.programs

import java.net.InetSocketAddress
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.net.{SocketAddress => JSocketAddr}

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import cats.syntax.applicative._
import com.comcast.ip4s.{Ipv4Address, SocketAddress}
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage.{GetUserKey, RegisterKey, SendMsgToUser, UserPublicKey}
import com.github.bromel777.mireaCrypto.services.{CertificationService, CipherService, KeyService, SocketService}
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import com.github.bromel777.mireaCrypto.utils.Blowfish
import fs2.concurrent.Queue
import scodec.bits.BitVector

trait NetworkProgram[F[_]] {

  def run: Stream[F, Unit]
}

object NetworkProgram {

  private final class Live[F[_]: Concurrent: ContextShift: Logger]
  (socketGroup: SocketGroup,
   toConnectQueue: Queue[F, SocketAddress[Ipv4Address]],
   toNetMsgsQueue: Queue[F, (UserMessage, InetSocketAddress)],
   activeConnections: Ref[F, Map[InetSocketAddress, SocketService[F]]],
   config: ApplicationSettings,
   certCervice: CertificationService[F],
   cipherService: CipherService[F],
  ) extends NetworkProgram[F] {

    private val startServer = (for {
      _           <- Stream.eval(Logger[F].info(s"Start server at port ${config.bindPort}"))
      socketRes   <- socketGroup.server(new InetSocketAddress(config.bindPort.value))
      _           <- Stream.eval(Logger[F].info(s"Got: ${socketRes}"))
      socket      <- Stream.resource(socketRes)
      addr        <- Stream.eval(socket.remoteAddress)
      service     <- Stream.resource(SocketService(socket, toNetMsgsQueue))
      _           <- Stream.eval(updateActiveConnection(addr.asInstanceOf[InetSocketAddress], service))
    } yield service.read).parJoinUnbounded.evalMap { case (msg, remote) =>
      for {
        _ <- Logger[F].info(s"Got ${msg} from ${remote}")
        _ <- proccessIncomingMsg(
          msg,
          remote,
          KeyService.fact
        )
      } yield ()
    }

    private def updateActiveConnection(addr: InetSocketAddress, socket: SocketService[F]): F[Unit] =
      activeConnections.update(prevMap => prevMap + (addr -> socket))

    private val simpleConnect: Stream[F, Unit] = (for {
      peer <- toConnectQueue.dequeue
      _ <- Stream.eval(Logger[F].info(s"Trying to connect with $peer"))
      service <- Stream.resource(SocketService(socketGroup, peer, toNetMsgsQueue))
      _ <- Stream.eval(updateActiveConnection(peer.toInetSocketAddress, service))
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
      case RegisterKey(publicKeyBytes, signatureBytes, userLogin) => for {
        _ <- Logger[F].info(s"Register key for client ${clientIp}")
        _ <- certCervice.registerKeyPair(
          factory.generatePublic(new X509EncodedKeySpec(publicKeyBytes.toByteArray)),
          signatureBytes.toByteArray,
          userLogin.toByteArray
        )
      } yield ()
      case SendMsgToUser(senderLogin, msgCyptherBytes) => for {
        key <- cipherService.deriveCipherKey(senderLogin.toByteArray.map(_.toChar).mkString)
        _ <- Logger[F].info(s"Receive msg: ${Blowfish.decrypt(msgCyptherBytes.toByteArray, key.toByteArray).map(_.toChar).mkString}")
      } yield ()
      case GetUserKey(loginBytes) => for {
        userKey <- certCervice.getUserPublicKey(loginBytes.toByteArray)
        _ <- Logger[F].info(s"send back to: ${clientIp}")
        _ <- toNetMsgsQueue.enqueue1(
          UserPublicKey(
            BitVector(userKey.getEncoded),
            loginBytes
          ) -> clientIp.asInstanceOf[InetSocketAddress]
        )
      } yield ()
      case UserPublicKey(publicKeyBytes, userLogin) => for {
        _ <- Logger[F].info(s"Receive public key for user: ${userLogin.toByteArray.map(_.toChar).mkString}. ${
          KeyService.fact.generatePublic(new X509EncodedKeySpec(publicKeyBytes.toByteArray))}")
        _ <- certCervice.saveUserKey(publicKeyBytes.toByteArray, userLogin.toByteArray)
      } yield ()

      case a => Logger[F].info(s"oh: ${a}")
    }

    override def run: Stream[F, Unit] = startServer concurrently simpleConnect
  }

  def apply[F[_]: Concurrent: ContextShift: Logger](config: ApplicationSettings,
                                                    toNetMsgsQueue: Queue[F, (UserMessage, InetSocketAddress)],
                                                    certService: CertificationService[F],
                                                    cipherService: CipherService[F]): Resource[F, NetworkProgram[F]] =
    Blocker[F].flatMap { blocker =>
      SocketGroup[F](blocker).evalMap { socketGroup =>
        for {
          queue <- Queue.bounded[F, SocketAddress[Ipv4Address]](100)
          activeConnections <- Ref.of[F, Map[InetSocketAddress, SocketService[F]]](Map.empty[InetSocketAddress, SocketService[F]])
          _ <- (config.certificationCenterAddr :: config.knownPeers).traverse(peer => queue.enqueue1(peer))
        } yield new Live(socketGroup, queue, toNetMsgsQueue, activeConnections, config, certService, cipherService)
      }
    }
}
