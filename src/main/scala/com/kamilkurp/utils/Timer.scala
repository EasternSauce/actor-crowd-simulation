package com.kamilkurp.utils

class Timer(val timeout: Int) {
  var time: Int = 0
  var running: Boolean = true

  def start(): Unit = {
    running = true
  }

  def update(delta: Int): Unit = {
    if (running) time += delta
  }

  def stop(): Unit = {
    running = false
  }

  def timedOut(): Boolean = {
    time > timeout
  }

  def set(time: Int): Unit = {
    this.time = time
  }

  def reset(): Unit = {
    time = 0
  }
}
