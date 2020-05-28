package com.github.bromel777.mireaCrypto.settings

import com.typesafe.config.ConfigFactory
import com.comcast.ip4s.{Ipv4Address, Port, SocketAddress}
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader

case class ApplicationSettings(bindPort: Port,
                               knownPeers: List[SocketAddress[Ipv4Address]])

object ApplicationSettings {

  val configPath: String = "mirea"

  implicit val inetSocketAddressReader: ValueReader[SocketAddress[Ipv4Address]] = { (config: Config, path: String) =>
    val split = config.getString(path).split(":")
    SocketAddress(Ipv4Address(split(0)).get, Port(split(1).toInt).get)
  }

  implicit val portReaders: ValueReader[Port] = { (conf: Config, path: String) => Port(conf.getInt(path)).get}

  def loadConfig(configName: String): ApplicationSettings =
    ConfigFactory
      .load(configName)
      .withFallback(ConfigFactory.load())
      .as[ApplicationSettings](configPath)
}
