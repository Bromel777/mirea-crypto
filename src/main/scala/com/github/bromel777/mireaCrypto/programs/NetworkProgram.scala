package com.github.bromel777.mireaCrypto.programs

import fs2.Stream

trait NetworkProgram[F[_]] {

  def run: Stream[F, Unit]
}
