package com.kamilkurp.agent

import com.kamilkurp.utils.Timer

import scala.util.Random

trait Follower {

  var followX: Float = 0
  var followY: Float = 0
  var followDistance: Float = 0

  var followedAgent: Agent = _

  val followTimer: Timer = new Timer(5000)

  val lastSeenFollowedEntityTimer = new Timer(1000 + new Random().nextInt(600))

  var lostSightOfFollowedEntity: Boolean = false

}
