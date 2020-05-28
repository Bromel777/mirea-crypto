package com.github.bromel777.mireaCrypto.programs

import java.net.InetSocketAddress

import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import cats.syntax.applicative._
import com.comcast.ip4s.{Ipv4Address, SocketAddress}
import com.github.bromel777.mireaCrypto.services.SocketService
import com.github.bromel777.mireaCrypto.settings.ApplicationSettings
import fs2.concurrent.Queue

trait NetworkProgram[F[_]] {

  def run: Stream[F, Unit]
}

object NetworkProgram {

  private final class Live[F[_]: Concurrent: ContextShift: Logger]
  (socketGroup: SocketGroup,
   toConnectQueue: Queue[F, SocketAddress[Ipv4Address]],
   config: ApplicationSettings
  ) extends NetworkProgram[F] {

    private val startServer = for {
      _           <- Stream.eval(Logger[F].info(s"Start server at port ${config.bindPort}"))
      _           <- socketGroup.server(new InetSocketAddress(config.bindPort.value))
    } yield ()

    private val simpleConnect: Stream[F, Unit] = (for {
      peer <- toConnectQueue.dequeue
      _ <- Stream.eval(Logger[F].info(s"Trying to connect with $peer"))
      service <- Stream.resource(SocketService(socketGroup, peer))
    } yield service.read).parJoinUnbounded.evalMap(msg => Logger[F].info(s"Got ${msg}"))

    override def run: Stream[F, Unit] = startServer concurrently simpleConnect
  }

  def apply[F[_]: Concurrent: ContextShift: Logger](config: ApplicationSettings): Resource[F, NetworkProgram[F]] =
    Blocker[F].flatMap { blocker =>
      SocketGroup[F](blocker).evalMap { socketGroup =>
        for {
          queue <- Queue.bounded[F, SocketAddress[Ipv4Address]](100)
        } yield new Live(socketGroup, queue, config)
      }
    }
}
