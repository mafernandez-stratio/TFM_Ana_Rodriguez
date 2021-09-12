package org.example

import akka.actor.{Actor, ActorSystem, Props}

object Pingpong extends App {

  class Actor1 extends Actor {
    def receive = {
      case "a" =>
        sender ! "b"
      case "Ping" =>
        println("Pong")
    }
  }

  val system = ActorSystem("System1")
  val actor = system.actorOf(Props[Actor1], "Actor1")

  //system.terminate()

}
