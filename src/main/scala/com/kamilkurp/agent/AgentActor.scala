package com.kamilkurp.agent

import akka.actor.{Actor, ActorLogging}
import com.kamilkurp.behavior.{FollowBehavior, IdleBehavior, LeaderBehavior, SearchExitBehavior}
import com.kamilkurp.building.Door
import com.kamilkurp.entity.Entity
import com.kamilkurp.util.Configuration
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

      if (!agent.goTowardsDoor) {
        if (agent.behaviorManager.currentBehavior != LeaderBehavior.name) {
          if (that.behaviorManager.currentBehavior == LeaderBehavior.name) {
            agent.behaviorManager.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
            agent.followManager.lostSightOfFollowedEntity = false
            agent.followManager.lastSeenFollowedEntityTimer.reset()
            agent.followManager.lastSeenFollowedEntityTimer.start()
          }
        }
      }



    case AgentEnteredDoor(that, door, locationX, locationY) =>
      if (that.behaviorManager.currentBehavior == LeaderBehavior.name) {
        agent.doorToEnter = door
        agent.behaviorManager.follow(that, locationX, locationY, 0)
        agent.followManager.followTimer.start()
        agent.followManager.followTimer.reset()
        agent.goTowardsDoor = true
      }

    case AgentLeading(entity, locationX, locationY) =>
      if (agent.behaviorManager.currentBehavior == IdleBehavior.name || agent.behaviorManager.currentBehavior == SearchExitBehavior.name || agent.behaviorManager.currentBehavior == FollowBehavior.name) {

        val normalVector = new Vector2f(locationX - agent.shape.getCenterX, locationY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.walkAngle = normalVector.getTheta.floatValue()
        agent.viewAngle = normalVector.getTheta.floatValue()
      }




    case MoveOutOfTheWay(entity, delta) =>
      if (!agent.movingOutOfTheWay) {
        agent.movingOutOfTheWay = true
        agent.outOfWayTimer.reset()
        agent.outOfWayTimer.start()

        val normalVector = new Vector2f(entity.currentVelocityX, entity.currentVelocityY)
        normalVector.normalise()

        val randomValue = Random.nextInt(2)

        if (randomValue == 1) {
          normalVector.setTheta(normalVector.getTheta + 90 - 30)
        }
        else {
          normalVector.setTheta(normalVector.getTheta - 90 + 30)
        }

        agent.currentVelocityX = Configuration.AGENT_SPEED * normalVector.x * delta
        agent.currentVelocityY = Configuration.AGENT_SPEED * normalVector.y * delta
      }


  }


}
