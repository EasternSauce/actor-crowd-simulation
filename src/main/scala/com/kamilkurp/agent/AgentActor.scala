package com.kamilkurp.agent

import akka.actor.{Actor, ActorLogging}
import com.kamilkurp.behavior._
import com.kamilkurp.building.Door
import com.kamilkurp.entity.Entity
import com.kamilkurp.flame.Flames
import org.jgrapht.graph.DefaultWeightedEdge
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable.ListBuffer
import scala.util.Random

case class AgentWithinVision(entity: Entity, distance: Float)

case class AgentEnteredDoor(agent: Agent, door: Door, locationX: Float, locationY: Float)

case class AgentLeading(agent: Agent, locationX: Float, locationY: Float)

case class FireWithinVision(flames: Flames, locationX: Float, locationY: Float)


class AgentActor(val name: String, val agent: Agent) extends Actor with ActorLogging {

  val char: Agent = agent


  override def receive: Receive = {
    case AgentWithinVision(that: Agent, distance: Float) =>

      if (agent.currentBehavior.name != LeaderBehavior.name) {
        if (that.currentBehavior.name == LeaderBehavior.name) {
          if (agent.followModule.followedAgent == null) agent.follow(that, that.shape.getCenterX, that.shape.getCenterY, 120)
        }
      }



    case AgentEnteredDoor(that, door, locationX, locationY) =>
//      if (that.currentBehavior.name == LeaderBehavior.name) {
//        agent.doorToEnter = door
//        agent.behaviorModule.follow(that, locationX, locationY, 0)
//        agent.followModule.followTimer.start()
//        agent.followModule.followTimer.reset()
//        agent.movementModule.goTowardsDoor = true
//      }

    case AgentLeading(entity, locationX, locationY) =>
      if (agent.currentBehavior.name == IdleBehavior.name || agent.currentBehavior.name == SearchExitBehavior.name || agent.currentBehavior.name == FollowBehavior.name) {

        val normalVector = new Vector2f(locationX - agent.shape.getCenterX, locationY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.movementModule.walkAngle = normalVector.getTheta.floatValue()
        //agent.viewAngle = normalVector.getTheta.floatValue()
      }

    case FireWithinVision(flames, locationX, locationY) =>



//      println(agent.name + " saw fire!")

      //agent.behaviorManager.setBehavior(AvoidFireBehavior.name)

      val edgeArray = agent.weightedGraph.edgeSet().toArray

      var toRemove: ListBuffer[DefaultWeightedEdge] = new ListBuffer[DefaultWeightedEdge]()
      for (edgeRef <- edgeArray){
        val edge = edgeRef.asInstanceOf[DefaultWeightedEdge]

        if(agent.doorToEnter != null) {
          if (agent.weightedGraph.getEdgeSource(edge) == agent.room && agent.weightedGraph.getEdgeTarget(edge) == agent.doorToEnter.leadingToDoor.room) {
//            println("removing edge " + agent.weightedGraph.getEdgeSource(edge).name + " " + agent.weightedGraph.getEdgeTarget(edge).name)
            toRemove += edge
          }
        }
      }

      for (edge <- toRemove) {

          if (agent.debug) {
            println("removed edge " + agent.weightedGraph.getEdgeSource(edge) + " " + agent.weightedGraph.getEdgeTarget(edge))
          }

//        if (agent.weightedGraph.getEdgeTarget(edge).meetPointList.nonEmpty) {
//          agent.setBehavior(AvoidFireBehavior.name)
//        }
        agent.weightedGraph.removeEdge(edge)

      }

      if (agent.currentBehavior.name == LeaderBehavior.name) agent.doorToEnter = agent.findDoorToEnterNext()
      //if (agent.currentBehavior.name == AvoidFireBehavior.name) agent.doorToEnter = agent.
  }
}
