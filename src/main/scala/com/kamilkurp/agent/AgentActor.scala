package com.kamilkurp.agent

import akka.actor.{Actor, ActorLogging}
import com.kamilkurp.building.Door
import com.kamilkurp.entity.Entity
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

case class AgentWithinVision(entity: Entity, distance: Float, delta: Float)

case class AgentEnteredDoor(character: Agent, door: Door, locationX: Float, locationY: Float)

case class AgentLeading(character: Agent, locationX: Float, locationY: Float)

case class MoveOutOfTheWay(entity: Entity, delta: Float)

class AgentActor(val name: String, val character: Agent) extends Actor with ActorLogging {

  val char: Agent = character


  override def receive: Receive = {
    case AgentWithinVision(that: Agent, distance: Float, delta: Float) =>

      if (character.currentBehavior == "idle" || character.currentBehavior == "follow") {
        if (that.currentBehavior == "leader" && that.followedAgent == null) {
          character.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
          character.lostSightOfFollowedEntity = false
          character.lastSeenFollowedEntityTimer.reset()
        } else if (that.currentBehavior == "follow") {
          if (character.followedAgent == null || character.lostSightOfFollowedEntity) {


            var loopDetected: Boolean = false
            var followChain: Agent = that
            var lastCharacter: Agent = that

            while (followChain != null) {
              lastCharacter = followChain


              if (followChain == character || followChain.lostSightOfFollowedEntity) {
                loopDetected = true
                followChain = null
              }
              else {
                followChain = followChain.followedAgent
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
        if (character.currentBehavior == "leader" || (character.currentBehavior == "follow" && that.followedAgent == character)) {
          that.actor ! MoveOutOfTheWay(character, delta)
        }
      }

    case AgentEnteredDoor(that, door, locationX, locationY) =>
      if (character.followedAgent == that) {
        character.doorToEnter = door
        character.follow(that, locationX, locationY, 0)
        character.getBehavior("follow").timer.stop()
      }

    case AgentLeading(entity, locationX, locationY) =>
      if (character.currentBehavior == "idle" ||
        (character.currentBehavior == "follow" && (character.followedAgent == null || character.followedAgent == entity)) ||
        (character.currentBehavior == "follow" && character.lostSightOfFollowedEntity && !entity.lostSightOfFollowedEntity)) {

        var loopDetected: Boolean = false
        var followChain: Agent = entity
        var lastCharacter: Agent = entity

        while (followChain != null) {
          lastCharacter = followChain

          if (followChain == character || followChain.lostSightOfFollowedEntity) {
            loopDetected = true
            followChain = null
          }
          else {
            followChain = followChain.followedAgent
          }
        }

        if (!loopDetected) {
          val normalVector = new Vector2f(locationX - character.shape.getCenterX, locationY - character.shape.getCenterY)
          normalVector.normalise()

          character.walkAngle = normalVector.getTheta.floatValue()
          character.viewAngle = normalVector.getTheta.floatValue()
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
