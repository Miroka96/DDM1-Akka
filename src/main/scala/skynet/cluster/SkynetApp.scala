package skynet.cluster

import java.net.{InetAddress, UnknownHostException}

import com.beust.jcommander.{JCommander, Parameter, ParameterException, Parameters}


object SkynetApp {
  val ACTOR_SYSTEM_NAME = "skynet"

  def main(args: Array[String]): Unit = {
    val masterCommand = new MasterCommand
    val slaveCommand = new SlaveCommand

    val jCommander = JCommander
      .newBuilder
      .addCommand(SkynetMaster.MASTER_ROLE, masterCommand)
      .addCommand(SkynetSlave.SLAVE_ROLE, slaveCommand)
      .build

    try {
      jCommander.parse(args:_*)

      if (jCommander.getParsedCommand == null) throw new ParameterException("No command given.")

      jCommander.getParsedCommand match {
        case SkynetMaster.MASTER_ROLE =>
          SkynetMaster.start(
            ACTOR_SYSTEM_NAME,
            masterCommand.workers,
            masterCommand.host,
            masterCommand.port)

        case SkynetSlave.SLAVE_ROLE =>
          SkynetSlave.start(
            ACTOR_SYSTEM_NAME,
            slaveCommand.workers,
            slaveCommand.host,
            slaveCommand.port,
            slaveCommand.masterhost,
            slaveCommand.masterport)

        case _ =>
          throw new AssertionError
      }
    } catch {
      case e: ParameterException =>
        System.out.printf("Could not parse args: %s\n", e.getMessage)

        if (jCommander.getParsedCommand == null) jCommander.usage()
        else jCommander.usage(jCommander.getParsedCommand)

        System.exit(1)
    }
  }
}

private[SkynetApp] object CommandBase {
  val DEFAULT_MASTER_PORT = 7877
  val DEFAULT_SLAVE_PORT = 7879
  val DEFAULT_WORKERS = 4
}

import skynet.cluster.CommandBase._

private[SkynetApp] abstract class CommandBase {
  @Parameter(names = Array("-h", "--host"), description = "this machine's host name or IP to bind against")
  private[SkynetApp] val host: String = this.getDefaultHost

  private[SkynetApp] def getDefaultHost: String = try
    InetAddress.getLocalHost.getHostAddress
  catch {
    case _: UnknownHostException =>
      "localhost"
  }

  @Parameter(names = Array("-p", "--port"), description = "port to bind against", required = false)
  private[SkynetApp] val port: Int = this.getDefaultPort

  private[SkynetApp] def getDefaultPort :Int

  @Parameter(
    names = Array("-w", "--workers"),
    description = "number of workers to start locally",
    required = false)
  private[SkynetApp] val workers = DEFAULT_WORKERS
}

@Parameters(commandDescription = "start a master actor system")
private[SkynetApp] class MasterCommand extends CommandBase {
  override private[SkynetApp] def getDefaultPort: Int = DEFAULT_MASTER_PORT
}

@Parameters(commandDescription = "start a slave actor system")
private[SkynetApp] class SlaveCommand extends CommandBase {
  override private[SkynetApp] def getDefaultPort: Int = DEFAULT_SLAVE_PORT

  @Parameter(names = Array("-mp", "--masterport"), description = "port of the master", required = false)
  private[SkynetApp] val masterport = DEFAULT_MASTER_PORT
  @Parameter(names = Array("-mh", "--masterhost"), description = "host name or IP of the master", required = true)
  private[SkynetApp] val masterhost = null
}
