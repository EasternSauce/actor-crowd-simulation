package com.kamilkurp.simulation

import com.kamilkurp.utils.Globals

object Main extends App {

  import org.newdawn.slick.AppGameContainer

  val gameContainer = new AppGameContainer(new Simulation("Simple Slick Game"))
  gameContainer.setDisplayMode(Globals.WINDOW_X, Globals.WINDOW_Y, false)
  gameContainer.start()
}
