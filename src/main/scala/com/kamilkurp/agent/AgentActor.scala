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
        if (agent.currentBehavior != LeaderBehavior.name) {
          if (that.currentBehavior == LeaderBehavior.name) {
            agent.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
            agent.lostSightOfFollowedEntity = false
            agent.lastSeenFollowedEntityTimer.reset()
            agent.lastSeenFollowedEntityTimer.start()
          }

        }

//        if (that.currentBehavior == FollowBehavior.name && distance < 400) {
//          if (agent.currentBehavior == LeaderBehavior.name || (agent.currentBehavior == FollowBehavior.name && that.followedAgent == agent)) {
//            //that.actor ! MoveOutOfTheWay(agent, delta)
//          }
//        }
      }



    case AgentEnteredDoor(that, door, locationX, locationY) =>
//      if (agent.followedAgent == that) {

      if (that.currentBehavior == LeaderBehavior.name) {
        agent.doorToEnter = door
        agent.follow(that, locationX, locationY, 0)
        agent.followTimer.start()
        agent.followTimer.reset()
        agent.goTowardsDoor = true
      }

//      }

    case AgentLeading(entity, locationX, locationY) =>
      if (agent.currentBehavior == IdleBehavior.name || agent.currentBehavior == SearchExitBehavior.name || agent.currentBehavior == FollowBehavior.name) {

        val normalVector = new Vector2f(locationX - agent.shape.getCenterX, locationY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.walkAngle = normalVector.getTheta.floatValue()
        agent.viewAngle = normalVector.getTheta.floatValue()

//        println("agent leading")
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
