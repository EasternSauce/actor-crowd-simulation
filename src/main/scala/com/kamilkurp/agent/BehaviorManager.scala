package com.kamilkurp.agent

import com.kamilkurp.behaviors._
import com.kamilkurp.utils.Configuration

import scala.collection.mutable
import scala.util.Random

trait BehaviorManager {
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
      removeRandomRooms()
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

}
