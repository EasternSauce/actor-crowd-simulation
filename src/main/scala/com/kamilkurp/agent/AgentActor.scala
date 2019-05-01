package com.kamilkurp.agent

import akka.actor.{Actor, ActorLogging}
import com.kamilkurp.behavior._
import com.kamilkurp.building.Room
import com.kamilkurp.entity.Entity
import com.kamilkurp.flame.Flames
import org.jgrapht.graph.DefaultWeightedEdge

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AgentMessage

case class AgentWithinVision(entity: Entity, distance: Float) extends AgentMessage

case class AgentLeading(agent: Agent, locationX: Float, locationY: Float) extends AgentMessage

case class FireWithinVision(flames: Flames, locationX: Float, locationY: Float) extends AgentMessage

case class FireLocationInfo(fireLocations: mutable.Set[Room]) extends AgentMessage


class AgentActor(val name: String, val agent: Agent) extends Actor with ActorLogging {

  val char: Agent = agent


  override def receive: Receive = {
    case AgentWithinVision(that: Agent, distance: Float) =>

      if (agent.currentBehavior.name != LeaderBehavior.name) {
        if (that.currentBehavior.name == LeaderBehavior.name) {
          if (agent.followedAgent == null) {
            agent.followLeader(that)
          }
        }
      }

    case AgentLeading(that, locationX, locationY) =>
      if (agent.currentBehavior.name == IdleBehavior.name || agent.currentBehavior.name == SearchExitBehavior.name || agent.currentBehavior.name == FollowBehavior.name) {
        if (agent.currentBehavior.name != LeaderBehavior.name) {
          if (that.currentBehavior.name == LeaderBehavior.name) {
            if (agent.followedAgent == null) {
              agent.followLeader(that)
            }
          }
        }

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
        agent.mentalMapGraph.removeEdge(edge)

      }

      agent.knownFireLocations += agent.room

      if (agent.currentBehavior.name == LeaderBehavior.name) agent.doorToEnter = agent.findDoorToEnterNext()

    case FireLocationInfo(fireLocations: mutable.Set[Room]) =>
      fireLocations.foreach(location => {
        if(!agent.knownFireLocations.contains(location)) {
          agent.knownFireLocations += location
          agent.doorToEnter = agent.findDoorToEnterNext()
        }
      })

  }
}
