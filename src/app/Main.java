package app;

import ui.LoginScreen;
import util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class Main {
    public static void main(String[] args) {
    	
    	try (Connection con = DBConnection.getConnection()) {
    	    System.out.println("Connected successfully!");
    	} catch (SQLException e) {
    	    System.out.println("Connection failed: " + e.getMessage());
    	}
    	
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {}
            new LoginScreen().setVisible(true);
        });
    }
    
    
}
