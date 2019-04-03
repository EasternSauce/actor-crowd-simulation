package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.kamilkurp.entities.{Character, Door, Entity}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

case class CharacterWithinVision(entity: Entity, distance: Float, delta: Float)

case class CharacterEnteredDoor(character: Character, door: Door, locationX: Float, locationY: Float)

case class CharacterLeading(character: Character, locationX: Float, locationY: Float)

case class MoveOutOfTheWay(entity: Entity, delta: Float)

class CharacterActor(val name: String, val character: Character) extends Actor with ActorLogging {

  val char: entities.Character = character


  override def receive: Receive = {
    case CharacterWithinVision(that: Character, distance: Float, delta: Float) =>

      if (character.currentBehavior == "idle" || character.currentBehavior == "follow") {
        if (that.currentBehavior == "leader" && that.followedCharacter == null) {
          character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
          character.lostSightOfFollowedEntity = false
          character.lastSeenFollowedEntityTimer.reset()
        } else if (that.currentBehavior == "follow") {
          if (character.followedCharacter == null || character.lostSightOfFollowedEntity) {


            var loopDetected: Boolean = false
            var followChain: Character = that
            var lastCharacter: Character = that

            while (followChain != null) {
              lastCharacter = followChain


              if (followChain == character || followChain.lostSightOfFollowedEntity) {
                loopDetected = true
                followChain = null
              }
              else {
                followChain = followChain.followedCharacter
              }
            }





            if (!loopDetected && lastCharacter.currentBehavior == "leader") {
              character.lostSightOfFollowedEntity = false
              character.lastSeenFollowedEntityTimer.reset()
              character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
            }
          }
        }


      }

      if (that.currentBehavior == "follow" && distance < 400) {
        if (character.currentBehavior == "leader" || (character.currentBehavior == "follow" && that.followedCharacter == character)) {
          that.actor ! MoveOutOfTheWay(character, delta)
        }
      }

    case CharacterEnteredDoor(that, door, locationX, locationY) => {
      if (character.followedCharacter == that) {
        character.doorToEnter = door
        character.follow(that, locationX, locationY, 0)
        character.getBehavior("follow").timer.stop()
      }
    }

    case CharacterLeading(entity, locationX, locationY) => {
      if (character.currentBehavior == "idle" ||
        (character.currentBehavior == "follow" && (character.followedCharacter == null || character.followedCharacter == entity)) ||
        (character.currentBehavior == "follow" && character.lostSightOfFollowedEntity && !entity.lostSightOfFollowedEntity)) {

        var loopDetected: Boolean = false
        var followChain: Character = entity
        var lastCharacter: Character = entity

        while (followChain != null) {
          lastCharacter = followChain

          if (followChain == character || followChain.lostSightOfFollowedEntity) {
            loopDetected = true
            followChain = null
          }
          else {
            followChain = followChain.followedCharacter
          }
        }

        if (!loopDetected) {
          val normalVector = new Vector2f(locationX - character.shape.getCenterX, locationY - character.shape.getCenterY)
          normalVector.normalise()

          character.walkAngle = normalVector.getTheta.floatValue()
          character.viewAngle = normalVector.getTheta.floatValue()
        }


      }
    }

    case MoveOutOfTheWay(entity, delta) =>
      if (!character.movingOutOfTheWay) {
        character.movingOutOfTheWay = true
        character.outOfWayTimer.reset()

        val normalVector = new Vector2f(entity.currentVelocityX, entity.currentVelocityY)
        normalVector.normalise()

        val randomValue = Random.nextInt(2)

        if (randomValue == 1) {
          normalVector.setTheta(normalVector.getTheta + 90)
        }
        else {
          normalVector.setTheta(normalVector.getTheta - 90)
        }

        character.currentVelocityX = character.speed * normalVector.x * delta
        character.currentVelocityY = character.speed * normalVector.y * delta
      }


  }


}
