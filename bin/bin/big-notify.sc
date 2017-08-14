#!/usr/bin/env amm

import javax.swing._
import java.awt.{Color, BorderLayout}

@main
def main(msg: String): Unit = {
  val f = new JFrame("Attention!")
  f.getContentPane.setLayout(new BorderLayout)
  val label = new JLabel(s"<html><p>$msg</p></html>")
  label.setFont(label.getFont.deriveFont(42.0f))
  label.setHorizontalAlignment(SwingConstants.CENTER)
  f.getContentPane.add(label, BorderLayout.CENTER)
  f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  f.setSize(800, 800)
  f.setUndecorated(true)
  f.getContentPane.setBackground(Color.BLACK)
  label.setForeground(Color.RED)
  f.setVisible(true)

  while (true) {
    Thread.sleep(100)
  }
}
