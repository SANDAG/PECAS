package com.hbaspecto.pecas.aa;

import java.awt.Frame;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

import com.hbaspecto.pecas.aa.control.AAControl;

public class ControlDialog {

	public void displayMe() {
		final Frame frame = null;
		// TODO figure out how to get the default frame or make one.
		JOptionPane.showConfirmDialog(frame, "Do you want to stop the model?",
				"Model Interrupt Control", JOptionPane.YES_NO_OPTION);
	}

	public static void main(String[] args) {
		final InputStreamReader keyboardInput = new InputStreamReader(System.in);
		boolean stopping = false;
		do {
			try {
				Thread.sleep(5000);
				System.out.println("still going...");
			}
			catch (final InterruptedException e) {
				// TODO ok
			}
			stopping = stopping
					|| AAControl.checkToSeeIfUserWantsToStopModel(keyboardInput);
		} while (!stopping);
	}

}
