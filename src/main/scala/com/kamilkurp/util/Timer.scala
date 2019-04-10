package com.kamilkurp.util

import scala.collection.mutable.ListBuffer

class Timer(val timeout: Int) {

  var time: Int = 0
  var running: Boolean = false

  Timer.timerList += this

  def start(): Unit = {
    running = true
  }

  def stop(): Unit = {
    running = false
  }

  def timedOut(): Boolean = {
    time > timeout
  }

  def reset(): Unit = {
    time = 0
  }

}

object Timer {
  var timerList: ListBuffer[Timer] = ListBuffer[Timer]()

  def updateTimers(delta: Int): Unit = {
    for (timer <- timerList) {
      if (timer.running) timer.time += delta

    }
  }
}
