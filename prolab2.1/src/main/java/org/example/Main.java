package org.example;

import javax.swing.*;
import java.awt.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        String playerName = JOptionPane.showInputDialog("Player Name:");
        JFrame frame = new JFrame("Treasure Hunt Game");

        int grid = Integer.parseInt(JOptionPane.showInputDialog("Grid Size: "));
        TreasureHuntGame game = new TreasureHuntGame(grid, playerName);

        // JFrame boyutunu ayarla
        int frameWidth = grid * 30 +100;
        int frameHeight = grid * 30 + 100; // Ekstra alan için 100 eklendi

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameWidth, frameHeight);

        frame.add(game, BorderLayout.CENTER);
        frame.setVisible(true);


    }
}