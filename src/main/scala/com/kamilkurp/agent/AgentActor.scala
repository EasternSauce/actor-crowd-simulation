package com.kamilkurp.agent

import akka.actor.{Actor, ActorLogging}
import com.kamilkurp.building.Door
import com.kamilkurp.entity.Entity
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

case class AgentWithinVision(entity: Entity, distance: Float, delta: Float)

case class AgentEnteredDoor(agent: Agent, door: Door, locationX: Float, locationY: Float)

case class AgentLeading(agent: Agent, locationX: Float, locationY: Float)

case class MoveOutOfTheWay(entity: Entity, delta: Float)

class AgentActor(val name: String, val agent: Agent) extends Actor with ActorLogging {

  val char: Agent = agent


  override def receive: Receive = {
    case AgentWithinVision(that: Agent, distance: Float, delta: Float) =>

      if (agent.currentBehavior == "idle" || agent.currentBehavior == "follow") {
        if (that.currentBehavior == "leader" && that.followedAgent == null) {
          agent.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
          agent.lostSightOfFollowedEntity = false
          agent.lastSeenFollowedEntityTimer.reset()
        } else if (that.currentBehavior == "follow") {
          if (agent.followedAgent == null || agent.lostSightOfFollowedEntity) {


            var loopDetected: Boolean = false
            var followChain: Agent = that
            var lastAgent: Agent = that

            while (followChain != null) {
              lastAgent = followChain


              if (followChain == agent || followChain.lostSightOfFollowedEntity) {
                loopDetected = true
                followChain = null
              }
              else {
                followChain = followChain.followedAgent
              }
            }


            if (!loopDetected && lastAgent.currentBehavior == "leader") {
              agent.lostSightOfFollowedEntity = false
              agent.lastSeenFollowedEntityTimer.reset()
              agent.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
            }
          }
        }


      }

      if (that.currentBehavior == "follow" && distance < 400) {
        if (agent.currentBehavior == "leader" || (agent.currentBehavior == "follow" && that.followedAgent == agent)) {
          that.actor ! MoveOutOfTheWay(agent, delta)
        }
      }

    case AgentEnteredDoor(that, door, locationX, locationY) =>
      if (agent.followedAgent == that) {
        agent.doorToEnter = door
        agent.follow(that, locationX, locationY, 0)
        agent.followTimer.stop()
      }

    case AgentLeading(entity, locationX, locationY) =>
      if (agent.currentBehavior == "idle" ||
        (agent.currentBehavior == "follow" && (agent.followedAgent == null || agent.followedAgent == entity)) ||
        (agent.currentBehavior == "follow" && agent.lostSightOfFollowedEntity && !entity.lostSightOfFollowedEntity)) {

        var loopDetected: Boolean = false
        var followChain: Agent = entity
        var lastAgent: Agent = entity

        while (followChain != null) {
          lastAgent = followChain

          if (followChain == agent || followChain.lostSightOfFollowedEntity) {
            loopDetected = true
            followChain = null
          }
          else {
            followChain = followChain.followedAgent
          }
        }

        if (!loopDetected) {
          val normalVector = new Vector2f(locationX - agent.shape.getCenterX, locationY - agent.shape.getCenterY)
          normalVector.normalise()

          agent.walkAngle = normalVector.getTheta.floatValue()
          agent.viewAngle = normalVector.getTheta.floatValue()
        }


      }

    case MoveOutOfTheWay(entity, delta) =>
      if (!agent.movingOutOfTheWay) {
        agent.movingOutOfTheWay = true
        agent.outOfWayTimer.reset()

        val normalVector = new Vector2f(entity.currentVelocityX, entity.currentVelocityY)
        normalVector.normalise()

        val randomValue = Random.nextInt(2)

        if (randomValue == 1) {
          normalVector.setTheta(normalVector.getTheta + 90)
        }
        else {
          normalVector.setTheta(normalVector.getTheta - 90)
        }

        agent.currentVelocityX = agent.speed * normalVector.x * delta
        agent.currentVelocityY = agent.speed * normalVector.y * delta
      }


  }


}
