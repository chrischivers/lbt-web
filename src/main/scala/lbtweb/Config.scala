package lbtweb

import com.typesafe.config.ConfigFactory

case class ServerConfig(url: String, port: Int)

case class LBTWebConfig(
                         serverConfig: ServerConfig
                       )

object ConfigLoader {

  private val defaultConfigFactory = ConfigFactory.load()

  val defaultConfig: LBTWebConfig = {
    val serverParamsPrefix = "server."
    LBTWebConfig(
      ServerConfig(
        defaultConfigFactory.getString(serverParamsPrefix + "url"),
        defaultConfigFactory.getInt(serverParamsPrefix + "port")
      )
    )

  }
}

