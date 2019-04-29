package com.kamilkurp.agent

import akka.actor.{Actor, ActorLogging}
import com.kamilkurp.behavior._
import com.kamilkurp.building.Door
import com.kamilkurp.entity.Entity
import com.kamilkurp.flame.Flames
import org.jgrapht.graph.DefaultWeightedEdge
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable.ListBuffer

case class AgentWithinVision(entity: Entity, distance: Float)

case class AgentLeading(agent: Agent, locationX: Float, locationY: Float)

case class FireWithinVision(flames: Flames, locationX: Float, locationY: Float)


class AgentActor(val name: String, val agent: Agent) extends Actor with ActorLogging {

  val char: Agent = agent


  override def receive: Receive = {
    case AgentWithinVision(that: Agent, distance: Float) =>

      if (agent.currentBehavior.name != LeaderBehavior.name) {
        if (that.currentBehavior.name == LeaderBehavior.name) {
          if (agent.followedAgent == null) {
            agent.followedAgent = that
            agent.followX = that.shape.getCenterX
            agent.followY = that.shape.getCenterY
            agent.followDistance = 120
            agent.setBehavior(FollowBehavior.name)
          }
        }
      }

    case AgentLeading(entity, locationX, locationY) =>
      if (agent.currentBehavior.name == IdleBehavior.name || agent.currentBehavior.name == SearchExitBehavior.name || agent.currentBehavior.name == FollowBehavior.name) {

        val normalVector = new Vector2f(locationX - agent.shape.getCenterX, locationY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.movementModule.walkAngle = normalVector.getTheta.floatValue()
      }

    case FireWithinVision(flames, locationX, locationY) =>

      val edgeArray = agent.mentalMapGraph.edgeSet().toArray

      var toRemove: ListBuffer[DefaultWeightedEdge] = new ListBuffer[DefaultWeightedEdge]()
      for (edgeRef <- edgeArray){
        val edge = edgeRef.asInstanceOf[DefaultWeightedEdge]

        if(agent.doorToEnter != null) {
          if (agent.mentalMapGraph.getEdgeSource(edge) == agent.room && agent.mentalMapGraph.getEdgeTarget(edge) == agent.doorToEnter.leadingToDoor.room) {
            toRemove += edge
          }
        }
      }

      for (edge <- toRemove) {

          if (agent.debug) {
            println("removed edge " + agent.mentalMapGraph.getEdgeSource(edge) + " " + agent.mentalMapGraph.getEdgeTarget(edge))
          }

        agent.mentalMapGraph.removeEdge(edge)

      }

      if (agent.currentBehavior.name == LeaderBehavior.name) agent.doorToEnter = agent.findDoorToEnterNext()
  }
}
