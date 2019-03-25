package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.kamilkurp.entities.{Character, Entity}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

case class Hello(sender: String)

case class UpdatePosition(delta: Int, actorList: List[ActorRef])

case class NearbyProbe(x: Int, y: Int)

case class CollisionProbe(x: Int, y: Int, w: Int, h: Int)

case class SomeoneNearby(name: String, x: Float, y: Float, w: Float, h: Float)

case class CharacterWithinVision(entity: Entity, distance: Float)

case class CharacterEnteredDoor(entity: Entity, locationX: Float, locationY: Float)

case class CharacterLeading(entity: Entity, locationX: Float, locationY: Float)

case class MoveOutOfTheWay(entity: Entity)

class CharacterActor(val name: String, val character: Character) extends Actor with ActorLogging {

  val char: entities.Character = character


  override def receive: Receive = {
    case NearbyProbe(thatX, thatY) if Math.abs(thatX - char.shape.getX) <= 25 && Math.abs(thatY - char.shape.getY) <= 25 && sender != self =>
      sender ! Hello(name)
    case CharacterWithinVision(that: Character, distance: Float) =>

      if (character.currentBehavior == "idle" || character.currentBehavior == "follow") {
        if (that.currentBehavior == "leader") {
          character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
        } else if (that.currentBehavior == "follow" && that.followingEntity == null) {
          if (character.followingEntity == null || character.getDistanceTo(that) < character.getDistanceTo(character.followingEntity)) {
            if (that.followingEntity != character) {
              character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
            }
          }
        }


      }

      if (that.currentBehavior == "follow" && distance < 150) {
        if (character.currentBehavior == "leader" || (character.currentBehavior == "follow" && that.followingEntity == character)) {
          that.actor ! MoveOutOfTheWay(character)
        }
      }

    case CharacterEnteredDoor(entity, locationX, locationY) => {
      if (character.followingEntity == entity) {
        character.follow(entity, locationX, locationY, 0)
      }
    }

    case CharacterLeading(entity, locationX, locationY) => {
      if (character.currentBehavior == "idle" || (character.currentBehavior == "follow" && character.followingEntity == entity)) {
        val normalVector = new Vector2f(locationX - character.shape.getCenterX, locationY - character.shape.getCenterY)
        normalVector.normalise()

        character.walkAngle = normalVector.getTheta.floatValue()
        character.viewAngle = normalVector.getTheta.floatValue()
      }
    }

    case MoveOutOfTheWay(entity) =>
      if (!character.movingOutOfTheWay) {
        character.movingOutOfTheWay = true
        character.outOfWayTimer = 0

        val normalVector = new Vector2f(entity.currentVelocityX, entity.currentVelocityY)
        normalVector.normalise()

        val randomValue = Random.nextInt(2)

        if (randomValue == 1) {
          normalVector.setTheta(normalVector.getTheta + 90)
        }
        else {
          normalVector.setTheta(normalVector.getTheta - 90)
        }

        character.currentVelocityX = character.speed * normalVector.x
        character.currentVelocityY = character.speed * normalVector.y
      }


  }


}

case object PrintPosition

case object CharacterInfo
