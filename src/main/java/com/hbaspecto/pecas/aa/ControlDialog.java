package com.hbaspecto.pecas.aa;
import java.awt.Component;
import java.awt.Frame;
import java.io.InputStreamReader;

import com.hbaspecto.pecas.aa.control.AAControl;

import javax.swing.JOptionPane;

public class ControlDialog {
	
	public void displayMe() {
		Frame frame = null;
		// TODO figure out how to get the default frame or make one.
		JOptionPane.showConfirmDialog(frame,"Do you want to stop the model?","Model Interrupt Control", JOptionPane.YES_NO_OPTION);
	}
	
	public static void main(String[] args) {
        InputStreamReader keyboardInput = new InputStreamReader(System.in);
		boolean stopping=false;
		do {
			try {
				Thread.sleep(5000);
				System.out.println("still going...");
			} catch (InterruptedException e) {
				// TODO ok
			}
			stopping = stopping || AAControl.checkToSeeIfUserWantsToStopModel(keyboardInput);
		} while (!stopping);
	}

}
