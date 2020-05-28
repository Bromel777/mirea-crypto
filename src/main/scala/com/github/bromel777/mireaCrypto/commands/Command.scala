package com.github.bromel777.mireaCrypto.commands

trait Command[F[_]] {

  val name: String

  def execute: F[Unit]
}
