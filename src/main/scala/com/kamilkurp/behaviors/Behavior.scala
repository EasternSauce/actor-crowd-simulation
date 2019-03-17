package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character

import scala.collection.mutable

abstract class Behavior {
  var timer: Int = 0
  var timerTimeout: Int

  def perform(character: Character, delta: Int): Unit

}

object Behavior {
  val behaviorMap: mutable.HashMap[String, Behavior] = mutable.HashMap.empty[String,Behavior]

  behaviorMap += ("following" -> new FollowingBehavior)
  behaviorMap += ("relaxed" -> new RelaxedBehavior)
  behaviorMap += ("runToExit" -> new RunToExitBehavior)

  def getBehavior(behaviorName: String): Behavior = behaviorMap(behaviorName)
}