package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.kamilkurp.entities.{Character, Door, Entity}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

case class CharacterWithinVision(entity: Entity, distance: Float)

case class CharacterEnteredDoor(character: Character, door: Door, locationX: Float, locationY: Float)

case class CharacterLeading(character: Character, locationX: Float, locationY: Float)

case class MoveOutOfTheWay(entity: Entity)

class CharacterActor(val name: String, val character: Character) extends Actor with ActorLogging {

  val char: entities.Character = character


  override def receive: Receive = {
    case CharacterWithinVision(that: Character, distance: Float) =>

//      if (character.currentVelocityX == 0 && character.currentVelocityY == 0) {
//        println(character.name + " standing in place, followedcharacter=" + (if(that.followedCharacter == null) "null" else that.followedCharacter.name) + " ")
//      }

      if (character.currentBehavior == "idle" || character.currentBehavior == "follow") {
        if (that.currentBehavior == "leader" && that.followedCharacter == null) {
//          println(name + " following leader")
          character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
          character.lostSightOfFollowedEntity = false
          character.lastSeenFollowedEntityTimer.reset()
        } else if (that.currentBehavior == "follow") {
          if (character.followedCharacter == null || character.lostSightOfFollowedEntity) {

//            println(character.name + "trying to follow non-leader")

            var loopDetected: Boolean = false
            var followChain: Character = that
            var lastCharacter: Character = that

//            var print = "chain for character " + character.name + ": "

            while (followChain != null) {
              lastCharacter = followChain

//              print += followChain.name + " "

              if (followChain == character || followChain.lostSightOfFollowedEntity) {
                loopDetected = true
                followChain = null
              }
              else {
                followChain = followChain.followedCharacter
              }
            }





            if (!loopDetected && lastCharacter.currentBehavior == "leader") {
//              println(print)
              character.lostSightOfFollowedEntity = false
              character.lastSeenFollowedEntityTimer.reset()
//              println(name + " following someone following leader")
              character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
            }
          }
        }


      }

      if (that.currentBehavior == "follow" && distance < 400) {
        if (character.currentBehavior == "leader" || (character.currentBehavior == "follow" && that.followedCharacter == character)) {
          that.actor ! MoveOutOfTheWay(character)
        }
      }

    case CharacterEnteredDoor(that, door, locationX, locationY) => {
      if (character.followedCharacter == that) {

//        character.allowChangeRoom = true
//        println(character.name + ": setting follow to door at " + locationX + " " + locationY)

        character.doorToEnter = door
        character.follow(that, locationX, locationY, 0)
        character.getBehavior("follow").timer.stop()
//        println(name + " following someone through door")
      }
    }

    case CharacterLeading(entity, locationX, locationY) => {
      if (character.currentBehavior == "idle" ||
        (character.currentBehavior == "follow" && (character.followedCharacter == null || character.followedCharacter == entity)) ||
        (character.currentBehavior == "follow" && character.lostSightOfFollowedEntity && !entity.lostSightOfFollowedEntity)) {
//      if (character.currentBehavior == "idle" || character.currentBehavior == "follow") {

//        if (character.currentBehavior == "follow" && character.lostSightOfFollowedEntity && !entity.lostSightOfFollowedEntity) {
//          println(character.name + " got message")
//        }

        var loopDetected: Boolean = false
        var followChain: Character = entity
        var lastCharacter: Character = entity

        //            var print = "chain for character " + character.name + ": "

        while (followChain != null) {
          lastCharacter = followChain

          //              print += followChain.name + " "

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

//          if (character.currentBehavior == "follow" && character.lostSightOfFollowedEntity && !entity.lostSightOfFollowedEntity) {
//            println(character.name + " actually looked")
//          }

          character.walkAngle = normalVector.getTheta.floatValue()
          character.viewAngle = normalVector.getTheta.floatValue()
        }


      }
    }

    case MoveOutOfTheWay(entity) =>
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

        character.currentVelocityX = character.speed * normalVector.x
        character.currentVelocityY = character.speed * normalVector.y
      }


  }


}
