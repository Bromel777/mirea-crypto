package com.github.bromel777.mireaCrypto.services

import cats.effect.Concurrent
import cats.implicits._
import com.github.bromel777.mireaCrypto.network.Protocol.UserMessage
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.Socket
import scodec.{Decoder, Encoder}
import scodec.stream.{StreamDecoder, StreamEncoder}

trait SocketService[F[_]] {
  def read: Stream[F, UserMessage]
  def write(msg: UserMessage): F[Unit]
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
  }

  def apply[F[_]: Concurrent](socket: Socket[F]): F[SocketService[F]] =
    for {
      queue <- Queue.bounded[F, UserMessage](100)
    } yield new Live(socket, queue)
}
