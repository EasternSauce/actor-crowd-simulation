package com.kamilkurp.agent

import com.kamilkurp.util.{Configuration, Timer}

import scala.util.Random

class FollowManager {
  private var _followX: Float = 0
  private var _followY: Float = 0
  var followDistance: Float = 0

  var followedAgent: Agent = _

  val followTimer: Timer = new Timer(Configuration.AGENT_FOLLOW_TIMER)

  val lastSeenFollowedEntityTimer = new Timer(1000 + new Random().nextInt(600))

  var lostSightOfFollowedEntity: Boolean = false

  def setFollow(x: Float, y: Float): Unit = {
    _followX = x
    _followY = y
  }

  def followX: Float = _followX
  def followY: Float = _followY
}
