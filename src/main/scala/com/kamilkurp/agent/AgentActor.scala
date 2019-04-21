package com.kamilkurp.agent

import akka.actor.{Actor, ActorLogging}
import com.kamilkurp.behavior.{FollowBehavior, IdleBehavior, LeaderBehavior, SearchExitBehavior}
import com.kamilkurp.building.Door
import com.kamilkurp.entity.Entity
import org.newdawn.slick.geom.Vector2f

case class AgentWithinVision(entity: Entity, distance: Float)

case class AgentEnteredDoor(agent: Agent, door: Door, locationX: Float, locationY: Float)

case class AgentLeading(agent: Agent, locationX: Float, locationY: Float)

class AgentActor(val name: String, val agent: Agent) extends Actor with ActorLogging {

  val char: Agent = agent


  override def receive: Receive = {
    case AgentWithinVision(that: Agent, distance: Float) =>

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
  }
}
