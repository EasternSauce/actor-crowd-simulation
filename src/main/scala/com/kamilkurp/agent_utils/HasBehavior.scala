package com.kamilkurp.agent_utils

import java.util

import com.kamilkurp.agent.Agent
import com.kamilkurp.behavior._
import com.kamilkurp.building.Room
import com.kamilkurp.util.Configuration
import org.jgrapht.alg.shortestpath.{AllDirectedPaths, KShortestSimplePaths}
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import com.kamilkurp.building.Room
import org.jgrapht.GraphPath

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

trait HasBehavior {
  this: Agent =>

  val behaviorMap: mutable.HashMap[String, Behavior] = mutable.HashMap.empty[String, Behavior]
  var currentBehavior: String = _




  def behaviorManagerInit(): Unit = {
    behaviorMap += (FollowBehavior.name -> new FollowBehavior(this, FollowBehavior.name, FollowBehavior.color))
    behaviorMap += (IdleBehavior.name -> new IdleBehavior(this, IdleBehavior.name, IdleBehavior.color))
    behaviorMap += (LeaderBehavior.name -> new LeaderBehavior(this, LeaderBehavior.name, LeaderBehavior.color))
    behaviorMap += (HoldMeetPointBehavior.name -> new HoldMeetPointBehavior(this, HoldMeetPointBehavior.name, HoldMeetPointBehavior.color))
    behaviorMap += (SearchExitBehavior.name -> new SearchExitBehavior(this, SearchExitBehavior.name, SearchExitBehavior.color))



    var startBehavior = IdleBehavior.name

    if (Random.nextFloat() < Configuration.LEADER_PERCENTAGE) {
      startBehavior = LeaderBehavior.name
    }

    if (name == Configuration.MANUAL_AGENT_NAME) {
      startBehavior = LeaderBehavior.name
    }

    setBehavior(startBehavior)

    if (currentBehavior != LeaderBehavior.name) {
      //removeRandomRooms()
      pickRandomPath()
    }

  }

  def setBehavior(behaviorName: String): Unit = {
    currentBehavior = behaviorName
    behaviorMap(behaviorName).init()
  }

  def follow(agent: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    getBehavior(currentBehavior).follow(agent, posX, posY, atDistance)
  }

  def getBehavior(behaviorName: String): Behavior = behaviorMap(behaviorName)

  def pickRandomPath(): Unit = {
    var meetPointRoom: Room = null

    val it: java.util.Iterator[Room] = roomGraph.vertexSet().iterator()
    while(it.hasNext) {
      val room = it.next()
      if (room.meetPointList.nonEmpty) meetPointRoom = room
    }

    if (meetPointRoom == room) return

    val shortestPaths: KShortestSimplePaths[Room,DefaultEdge] = new KShortestSimplePaths[Room, DefaultEdge](roomGraph)
    val paths: util.List[GraphPath[Room, DefaultEdge]] = shortestPaths.getPaths(room, meetPointRoom, 10)

    val randomPath: GraphPath[Room, DefaultEdge] = paths.get(Random.nextInt(paths.size()))


    val newGraph = new SimpleGraph[Room, DefaultEdge](classOf[DefaultEdge])

    val vertexIter: java.util.Iterator[Room] = randomPath.getVertexList.iterator()
    while(vertexIter.hasNext) {
      val room = vertexIter.next()
      newGraph.addVertex(room)
    }

    val edgeIter: java.util.Iterator[DefaultEdge] = randomPath.getEdgeList.iterator()
    while(edgeIter.hasNext) {
      val edge = edgeIter.next()
      newGraph.addEdge(roomGraph.getEdgeSource(edge), roomGraph.getEdgeTarget(edge))
    }

    roomGraph = newGraph
  }

  def removeRandomRooms(): Unit = {
    val agentRoomGraph = new SimpleGraph[Room, DefaultEdge](classOf[DefaultEdge])

    val toRemove: ListBuffer[Room] = new ListBuffer[Room]()
    val chanceToRemove = 0.25

    // copy graph
    val vertexIter: java.util.Iterator[Room] = roomGraph.vertexSet().iterator()
    while(vertexIter.hasNext) {
      val room = vertexIter.next()
      agentRoomGraph.addVertex(room)

      // fill list with random rooms to remove
      if (Random.nextFloat() < chanceToRemove) {
        toRemove += room
      }
    }

    val edgeIter: java.util.Iterator[DefaultEdge] = roomGraph.edgeSet().iterator()
    while(edgeIter.hasNext) {
      val edge = edgeIter.next()
      agentRoomGraph.addEdge(roomGraph.getEdgeSource(edge), roomGraph.getEdgeTarget(edge))
    }

    // remove random rooms
    for (room <- toRemove) {
      agentRoomGraph.removeVertex(room)
    }

    roomGraph = agentRoomGraph
  }

}
