package com.github.bromel777.mireaCrypto.services

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.implicits._
import com.comcast.ip4s.{Ipv4Address, SocketAddress}
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.{Socket, SocketGroup}
import io.chrisdavenport.log4cats.Logger
import scodec.{Decoder, Encoder}
import scodec.stream.{StreamDecoder, StreamEncoder}

trait SocketService[F[_]] {
  def read: Stream[F, UserMessage]
  def write(msg: UserMessage): F[Unit]
  def close: F[Unit]
}

object SocketService {

  private final class Live[F[_]: Concurrent](socket: Socket[F], queue: Queue[F, UserMessage]) extends SocketService[F] {

    override def read: Stream[F, UserMessage] =
      {
        val readStream = socket
          .reads(1024)
          .through(StreamDecoder.many(UserMessage.codec.asDecoder).toPipeByte)

        val writeStream = queue
          .dequeue
          .through(StreamEncoder.many(UserMessage.codec.asEncoder).toPipeByte)
          .through(socket.writes(None))

        readStream concurrently writeStream
      }

    override def write(msg: UserMessage): F[Unit] = queue.enqueue1(msg)

    override def close: F[Unit] = socket.close
  }

  def apply[F[_]: Concurrent: ContextShift: Logger](socketGroup: SocketGroup,
                                            peerIp: SocketAddress[Ipv4Address]): Resource[F, SocketService[F]] =
    socketGroup.client(peerIp.toInetSocketAddress).flatMap { socket =>
      Resource.make[F, SocketService[F]](
        Queue.bounded[F, UserMessage](100).map { queue =>
          new Live[F](socket, queue)
        })( _.close >> Logger[F].warn(s"Connection with ${peerIp} was closed!") )
    }
}
