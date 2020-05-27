package com.github.bromel777.mireaCrypto.services

trait ConsoleService[F[_]] {

  def executeCommand(command: String, args: List[String]): F[Unit]
}
