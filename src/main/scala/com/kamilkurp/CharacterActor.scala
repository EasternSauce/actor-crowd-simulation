package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.kamilkurp.entities.{Character, Entity}
import org.newdawn.slick.geom.Vector2f

case class Hello(sender: String)

case class UpdatePosition(delta: Int, actorList: List[ActorRef])

case class NearbyProbe(x: Int, y: Int)

case class CollisionProbe(x: Int, y: Int, w: Int, h: Int)

case class SomeoneNearby(name: String, x: Float, y: Float, w: Float, h: Float)

//case class SomeoneEvacuating(name: String, x: Float, y: Float, w: Float, h: Float)

case class OutOfTheWay(name: String, x: Float, y: Float, w: Float, h: Float)

case class CharacterWithinVision(entity: Entity, distance: Float)


case class CharacterEnteredDoor(entity: Entity, locationX: Float, locationY: Float)

case class CharacterLeadingEvacuation(entity: Entity, locationX: Float, locationY:Float)

class CharacterActor(val name: String, val character: Character) extends Actor with ActorLogging {

  val char: entities.Character = character


  override def receive: Receive = {
    //case Hello(sender) => log.info(sender + " says hello to " + name)
    case NearbyProbe(thatX, thatY) if Math.abs(thatX - char.shape.getX) <= 25 && Math.abs(thatY - char.shape.getY) <= 25 && sender != self =>
      sender ! Hello(name)
    case CharacterInfo => sender ! (name, char.shape.getX, char.shape.getY)

    case SomeoneNearby(name: String, x: Float, y: Float, w: Float, h: Float) => //println(this.name + " encounters " + name + " at " + x + ", " + y)

//    case SomeoneEvacuating(name: String, x: Float, y: Float, w: Float, h: Float) => {
      //println(this.name + " sees " + name + " evacuating at " + x + ", " + y)

//      var door = char.room.evacuationDoor
//
//      if (door != null) char.followingBehavior.start(door.posX + door.shape.getWidth/2, door.posY + door.shape.getHeight/2)
//    }

    case OutOfTheWay(name: String, x: Float, y: Float, w: Float, h: Float) => {
      //println(name + " screams to get out of the way to " + this.name)
    }

    case CharacterWithinVision(that: Character, distance: Float) => {
//      println(name + " sees " + that.name + " at distance " + distance)

      if (that.currentBehavior == "runToExit") {
        character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
      }
    }

    case CharacterEnteredDoor(entity, locationX, locationY) => {
//      println("character entered door: " + entity.name)
//      println("i am " + character.name + " following " + character.followingEntity.name)
      if (character.followingEntity == entity) {
//        println("gotta follow through door")
        character.follow(entity, locationX, locationY, 0)
      }
    }

    case CharacterLeadingEvacuation(entity, locationX, locationY) => {
//      println(character.name + " received broadcast from " + entity.name)
      if (character.currentBehavior == "following" && character.followingEntity == entity) {
        val normalVector = new Vector2f(locationX - character.shape.getCenterX, locationY - character.shape.getCenterY)
        normalVector.normalise()

        character.walkAngle = normalVector.getTheta.floatValue()
        character.viewAngle = normalVector.getTheta.floatValue()
      }

    }

  }


}

case object PrintPosition

case object CharacterInfo
