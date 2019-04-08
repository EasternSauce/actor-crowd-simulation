package com.kamilkurp.agent

import com.kamilkurp.behaviors._

import scala.collection.mutable
import scala.util.Random

trait BehaviorManager {
  this: Agent =>

  val behaviorMap: mutable.HashMap[String, Behavior] = mutable.HashMap.empty[String, Behavior]
  var currentBehavior: String = _


  def behaviorManagerInit(): Unit = {
    behaviorMap += ("follow" -> new FollowBehavior(this))
    behaviorMap += ("idle" -> new IdleBehavior(this))
    behaviorMap += ("leader" -> new LeaderBehavior(this))
    behaviorMap += ("holdMeetPoint" -> new HoldMeetPointBehavior(this))
    behaviorMap += ("searchExit" -> new SearchExitBehavior(this))

    var startBehavior = "idle"

//    println("chance is " + chanceToBeLeader)
    if (Random.nextFloat() < chanceToBeLeader) {
      startBehavior = "leader"
      //println("setting leader")
    }

    if (name == "Player") {
      startBehavior = "leader"
    }

    setBehavior(startBehavior)

    if (currentBehavior != "leader") {
      removeRandomRooms()
    }

  }

  def setBehavior(behaviorName: String): Unit = {
//    println("setting behavior " + behaviorName + " for " + name)
    currentBehavior = behaviorName
    behaviorMap(behaviorName).init()
  }

  def follow(agent: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    getBehavior(currentBehavior).follow(agent, posX, posY, atDistance)
  }

  def getBehavior(behaviorName: String): Behavior = behaviorMap(behaviorName)

}
