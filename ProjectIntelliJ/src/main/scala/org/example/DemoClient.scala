package org.example

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, ActorSystem, Props}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.example.Fail.fail
import org.example.Result.result

import scala.concurrent.{Await}
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object DemoClient {

  case class Query(msg: String)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.parseString("""
        akka {
          loglevel = "warning"
          actor {
            provider = "akka.cluster.ClusterActorRefProvider"
          }
          remote {
            transport = "akka.remote.netty.NettyRemoteTransport"
            netty.tcp {
              hostname = "127.0.0.1"
              port = 3000
            }
          }
          extensions = ["akka.cluster.client.ClusterClientReceptionist"]
          cluster.client {
            initial-contacts = ["akka.tcp://ClusterSystem@127.0.0.1:2551/system/receptionist"]
            establishing-get-contacts-interval = 3s
            refresh-contacts-interval = 60s
            heartbeat-interval = 2s
            acceptable-heartbeat-pause = 13s
            buffer-size = 1000
            reconnect-timeout = off
          }
        }
        """)

    val system = ActorSystem("ClientSystem", config)

    implicit val timeout: Timeout = Timeout(60 seconds)

    // create the client
    val initialContacts = Set(
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/system/receptionist"))
    val settings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val client = system.actorOf(ClusterClient.props(settings), "client")
    val clientActor = system.actorOf(Props(new ClientActor(client)), "clientActor")

    var message = ""
    while (message.toLowerCase() != "exit") {
      message = StdIn.readLine("Query: ")
      val sol = Await.result(clientActor ? Query(message), timeout.duration)
      println(sol)
    }
    system.terminate()

  }

  class ClientActor(clientActor: ActorRef) extends Actor with ActorLogging {
    implicit val timeout: Timeout = Timeout(60 seconds)
    var demoClient: ActorRef = _
    def receive: Receive = {
      case Query(message) =>
        demoClient = sender()
        clientActor ! ClusterClient.Send("/user/master", Query(message), localAffinity = false)
      case result(ans) =>
        demoClient ! result(ans)
      case fail(msg) =>
        demoClient ! fail(msg)

    }
  }
}

