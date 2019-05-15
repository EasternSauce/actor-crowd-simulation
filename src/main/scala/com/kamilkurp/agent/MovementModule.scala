package com.kamilkurp.agent

import com.kamilkurp.behavior.{AvoidFireBehavior, FollowBehavior, LeaderBehavior, SearchExitBehavior}
import com.kamilkurp.entity.Entity
import com.kamilkurp.util.{Configuration, ControlScheme, Globals, Timer}
import org.newdawn.slick.GameContainer
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class MovementModule {
  var currentVelocityX: Float = _
  var currentVelocityY: Float = _
  var changedVelocityX: Float = _
  var changedVelocityY: Float = _

  var changedVelocity: Boolean = _
  var walkAngle: Float = _
  var viewAngle: Float = _
  var slow: Float = _
  var beingPushed: Boolean = _
  var pushedTimer: Timer = _
  var checkProgressTimer: Timer = _
  var pastPositionX: Float = _
  var pastPositionY: Float = _
  var goAroundObstacle: Boolean = _
  var goAroundAngle: Float = _
  var slowTimer: Timer = _
  var lookTimer: Timer = _
  private var agent: Agent = _

  def moveTowards(x: Float, y: Float): Unit = {
    goTo(x, y)
  }

  private def goTo(x: Float, y: Float): Unit = {
    val vector = new Vector2f(x - agent.shape.getCenterX, y - agent.shape.getCenterY)
    vector.normalise()

    //    if (goAroundObstacle) {
    //      vector.setTheta(vector.getTheta + goAroundAngle)
    //    }

    vector.setTheta(vector.getTheta + agent.visionModule.colAvoidAngle * 1.5f)

    walkAngle = vector.getTheta.floatValue()

    if (!beingPushed) {
      currentVelocityX = vector.x * agent.personalSpeed * (1f - slow)
      currentVelocityY = vector.y * agent.personalSpeed * (1f - slow)
    }

  }

  def moveTowards(entity: Entity): Unit = {
    goTo(entity.shape.getCenterX, entity.shape.getCenterY)
  }

  def stopMoving(): Unit = {
    currentVelocityX = 0
    currentVelocityY = 0
  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    if (changedVelocity) {
      changedVelocity = false
      currentVelocityX = changedVelocityX
      currentVelocityY = changedVelocityY
    }

    if (checkProgressTimer.timedOut()) {
      checkProgressTimer.reset()

      val progress = agent.getDistanceTo(pastPositionX, pastPositionY)


      if (progress < 60) {
        goAroundObstacle = true
        goAroundAngle = Random.nextInt(60) - 30
      }
      else goAroundObstacle = false

      pastPositionX = agent.shape.getX
      pastPositionY = agent.shape.getY
    }

    if (lookTimer.timedOut() && walkAngle != viewAngle) {

      lookTimer.reset()

      def findSideToTurn(currentAngle: Float, desiredAngle: Float): Boolean = {
        var clockwise = false
        if (currentAngle < 180) {
          if (desiredAngle - currentAngle >= 0 && desiredAngle - currentAngle < 180) clockwise = true
          else clockwise = false
        }
        else {
          if (currentAngle - desiredAngle >= 0 && currentAngle - desiredAngle < 180) clockwise = false
          else clockwise = true

        }
        clockwise
      }


      def adjustViewAngle(clockwise: Boolean): Unit = {
        val turnSpeed = Configuration.agentTurnSpeed
        if (Math.abs(viewAngle - walkAngle) > turnSpeed && Math.abs((viewAngle + 180) % 360 - (walkAngle + 180) % 360) > turnSpeed) {
          if (clockwise) { // clockwise
            if (viewAngle + turnSpeed < 360) viewAngle += turnSpeed
            else viewAngle = viewAngle + turnSpeed - 360
          }
          else { // counterclockwise
            if (viewAngle - turnSpeed > 0) viewAngle -= turnSpeed
            else viewAngle = viewAngle - turnSpeed + 360
          }
        }
        else {
          viewAngle = walkAngle
        }
      }

      adjustViewAngle(findSideToTurn(viewAngle, walkAngle))

    }

    if (slowTimer.timedOut()) {
      slow = 0f
      slowTimer.stop()
    }

    if (pushedTimer.timedOut()) {
      beingPushed = false
      pushedTimer.stop()
    }

    if (agent.controlScheme == ControlScheme.Autonomous) {
      agent.currentBehavior.perform(delta)
    }
    else if (agent.controlScheme == ControlScheme.Manual) {
      ControlScheme.handleManualControls(agent, gc, delta, renderScale)
    }

    val collisionVelocityX = currentVelocityX * delta
    val collisionVelocityY = currentVelocityY * delta
    val collisionDetails = Globals.manageCollisions(agent.currentRoom, agent, collisionVelocityX, collisionVelocityY)
    if (!collisionDetails.colX) {
      agent.shape.setX(agent.shape.getX + collisionVelocityX)
    }
    if (!collisionDetails.colY) {
      agent.shape.setY(agent.shape.getY + collisionVelocityY)
    }

  }

  def pushBack(pusher: Agent, pushed: Agent): Unit = {
    if (pusher.currentBehavior.name == LeaderBehavior.name || pusher.currentBehavior.name == AvoidFireBehavior.name) {
      if (!pushed.movementModule.beingPushed) {
        val vector = new Vector2f(pusher.movementModule.currentVelocityX, pusher.movementModule.currentVelocityY)

        vector.setTheta(vector.getTheta + Random.nextInt(30) - 15)

        pushed.movementModule.changedVelocityX = vector.x
        pushed.movementModule.changedVelocityY = vector.y
        pushed.movementModule.changedVelocity = true

        pushed.movementModule.walkAngle = vector.getTheta.toFloat
        pushed.movementModule.viewAngle = vector.getTheta.toFloat

        pushed.movementModule.beingPushed = true
        pushed.movementModule.pushedTimer.reset()
        pushed.movementModule.pushedTimer.start()
      }

    }
    else if ((pusher.currentBehavior.name == FollowBehavior.name || pushed.currentBehavior.name == SearchExitBehavior.name) && pusher.movementModule.beingPushed) {
      if (!pushed.movementModule.beingPushed) {
        val vector = new Vector2f(currentVelocityX, currentVelocityY)

        vector.setTheta(vector.getTheta + Random.nextInt(30) - 15)

        pushed.movementModule.changedVelocityX = vector.x
        pushed.movementModule.changedVelocityY = vector.y
        pushed.movementModule.changedVelocity = true

        pushed.movementModule.walkAngle = vector.getTheta.toFloat
        pushed.movementModule.viewAngle = vector.getTheta.toFloat

        pushed.movementModule.beingPushed = true
        pushed.movementModule.pushedTimer.time = pusher.movementModule.pushedTimer.time
        pushed.movementModule.pushedTimer.start()
      }

    }
  }

}

object MovementModule {
  def apply(agent: Agent): MovementModule = {
    val movementModule = new MovementModule()

    movementModule.agent = agent

    movementModule.currentVelocityX = 0.0f
    movementModule.currentVelocityY = 0.0f

    movementModule.changedVelocityX = 0.0f
    movementModule.changedVelocityY = 0.0f
    movementModule.changedVelocity = false

    movementModule.walkAngle = 0.0f
    movementModule.viewAngle = 0.0f

    movementModule.slow = 0.0f
    movementModule.beingPushed = false

    movementModule.pushedTimer = new Timer(500)

    movementModule.checkProgressTimer = new Timer(2000)
    movementModule.checkProgressTimer.start()

    movementModule.pastPositionX = 0
    movementModule.pastPositionY = 0

    movementModule.goAroundObstacle = false
    movementModule.goAroundAngle = 0

    movementModule.slowTimer = new Timer(Configuration.AGENT_SLOW_TIMER)
    movementModule.lookTimer = new Timer(Configuration.AGENT_LOOK_TIMER)
    movementModule.slowTimer.start()
    movementModule.lookTimer.start()

    movementModule
  }

}