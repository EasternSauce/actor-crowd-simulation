package com.kamilkurp.simulation

import com.kamilkurp.util.{Configuration, Globals, Screen}
import org.newdawn.slick.gui.TextField
import org.newdawn.slick._
import org.newdawn.slick.geom.Rectangle

import scala.collection.mutable

class MainMenu private () {

  var params: mutable.LinkedHashMap[String, TextField] = _
  var font: Font = _
  var currentH: Int = 100
  var confirmButton: TextField = _
  var simulation: Simulation = _

  def update(gc: GameContainer, delta: Int): Unit = {

  }

  def draw(gc: GameContainer, g: Graphics): Unit = {
    g.setColor(Color.orange)
    params.foreach(pair => {
      val name = pair._1
      val textField = pair._2
      font.drawString(50, textField.getY, name + ":", Color.orange)
      textField.render(gc, g)
    })
    confirmButton.render(gc, g)
  }

  def onConfirm(): Unit = {
    Configuration.numberOfAgents = params("number of agents").getText.toInt
    Configuration.buildingPlanLocation = params("building plan location").getText
    Configuration.leaderPercentage = params("leader percentage").getText.toFloat
    Configuration.agentBroadcastTimer = params("agent broadcast timer").getText.toInt
    Configuration.untilAlarmTime = params("until alarm time").getText.toInt
    Configuration.flamePropagationSpeed = params("flame propagation speed").getText.toFloat
    Configuration.agentTurnSpeed = params("agent turn speed").getText.toInt
    Configuration.agentVisionTimer = params("agent vision timer").getText.toInt
    Configuration.agentBroadcastDistance = params("agent broadcast distance").getText.toInt
    //Configuration.leaderPercentage = Configuration.argument.toFloat


    simulation.setup()
  }

  def addField(text: String, value: String, gc: GameContainer): Unit = {
    val textField = new TextField(gc, font, 800, currentH, 700, 100)
    params.put(text, textField)
    textField.setText(value)
    textField.setCursorPos(textField.getText.length)

    currentH = currentH + 120
  }
}


object MainMenu {
  def apply(gc: GameContainer, renderScale: Float, simulation: Simulation): MainMenu = {
    val mainMenu: MainMenu = new MainMenu()

    mainMenu.simulation = simulation

    mainMenu.font = new TrueTypeFont(new java.awt.Font("Verdana", java.awt.Font.BOLD, (40 * 1 / renderScale).toInt), false)

    mainMenu.params = mutable.LinkedHashMap[String, TextField]()

    mainMenu.addField("number of agents", Configuration.numberOfAgents.toString, gc)
    mainMenu.addField("building plan location", Configuration.buildingPlanLocation.toString, gc)
    mainMenu.addField("leader percentage", Configuration.leaderPercentage.toString, gc)
    mainMenu.addField("agent broadcast timer", Configuration.agentBroadcastTimer.toString, gc)
    mainMenu.addField("until alarm time", Configuration.untilAlarmTime.toString, gc)
    mainMenu.addField("flame propagation speed", Configuration.flamePropagationSpeed.toString, gc)
    mainMenu.addField("agent turn speed", Configuration.agentTurnSpeed.toString, gc)
    mainMenu.addField("agent vision timer", Configuration.agentVisionTimer.toString, gc)
    mainMenu.addField("agent broadcast distance", Configuration.agentBroadcastDistance.toString, gc)

    mainMenu.confirmButton = new TextField(gc, mainMenu.font, 100, mainMenu.currentH+50, 1350, 100) {
      override def mousePressed(button: Int, x: Int, y: Int): Unit = {
        super.mousePressed(button, x, y)

        if (button == Input.MOUSE_LEFT_BUTTON) {
          val rect: Rectangle = new Rectangle(getX, getY, getWidth, getHeight)
          val mouseRect: Rectangle = new Rectangle(x, y, 1, 1)
          if (rect.intersects(mouseRect)) {
            Screen.currentScreen = Screen.Simulation
            mainMenu.onConfirm()
          }
        }

      }
    }

    mainMenu.confirmButton.setText("                                      CONFIRM")

    mainMenu
  }
}