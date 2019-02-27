package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef}

case class Hello(sender: String)

case class UpdatePosition(delta: Int, actorList: List[ActorRef])

case class NearbyProbe(x: Int, y: Int)

case class CollisionProbe(x: Int, y: Int, w: Int, h: Int)

case class SomeoneNearby(name: String, x: Float, y: Float, w: Float, h: Float)

class CharacterActor(val name: String, val character: Character) extends Actor with ActorLogging {

  val char: Character = character


  override def receive: Receive = {
    case Hello(sender) => log.info(sender + " says hello to " + name)
    case NearbyProbe(thatX, thatY) if Math.abs(thatX - char.x) <= 25 && Math.abs(thatY - char.y) <= 25 && sender != self =>
      sender ! Hello(name)
    case CharacterInfo => sender ! (name, char.x, char.y)

    case SomeoneNearby(name: String, x: Float, y: Float, w: Float, h: Float) => println(this.name + " encounters " + name + " at " + x + ", " + y)


  }


}

case object PrintPosition

case object CharacterInfo
