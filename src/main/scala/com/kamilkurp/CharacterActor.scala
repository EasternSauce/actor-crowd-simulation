package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef}

case class Hello(sender: String)

case class UpdatePosition(delta: Int, actorList: List[ActorRef])

case class NearbyProbe(x: Int, y: Int)

case class CollisionProbe(x: Int, y: Int, w: Int, h: Int)

case class SomeoneNearby(name: String, x: Float, y: Float, w: Float, h: Float)

case class SomeoneEvacuating(name: String, x: Float, y: Float, w: Float, h: Float)

case class OutOfTheWay(name: String, x: Float, y: Float, w: Float, h: Float)


class CharacterActor(val name: String, val character: entities.Character) extends Actor with ActorLogging {

  val char: entities.Character = character


  override def receive: Receive = {
    //case Hello(sender) => log.info(sender + " says hello to " + name)
    case NearbyProbe(thatX, thatY) if Math.abs(thatX - char.x) <= 25 && Math.abs(thatY - char.y) <= 25 && sender != self =>
      sender ! Hello(name)
    case CharacterInfo => sender ! (name, char.x, char.y)

    case SomeoneNearby(name: String, x: Float, y: Float, w: Float, h: Float) => //println(this.name + " encounters " + name + " at " + x + ", " + y)

    case SomeoneEvacuating(name: String, x: Float, y: Float, w: Float, h: Float) => {
      //println(this.name + " sees " + name + " evacuating at " + x + ", " + y)

      var door = char.room.evacuationDoor

      if (door != null) char.followingBehavior.start(door.x + door.w/2, door.y + door.h/2)
    }

    case OutOfTheWay(name: String, x: Float, y: Float, w: Float, h: Float) => {
      //println(name + " screams to get out of the way to " + this.name)
      char.moveAwayFrom(x + w/2, y + h/2)
    }



  }


}

case object PrintPosition

case object CharacterInfo
