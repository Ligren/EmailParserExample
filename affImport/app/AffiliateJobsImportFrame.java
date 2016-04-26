package com.example.affImport.app;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.example.affImport.mail.UniversalMBox;
import com.expl.dblib.accessrights.AccessRights;
import com.expl.dblib.accessrights.AccessRights.UserType;

@SuppressWarnings("serial")
public class AffiliateJobsImportFrame extends JFrame {

	private Color colorActive = new Color(0, 150, 0);

	JPanel contentPane;
	JMenuBar menuBarMain = new JMenuBar();
	JLabel lbl_POP3Error = new JLabel();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JPanel jPanelProcessIncoming = new JPanel();
	GridBagLayout gridBagLayout4 = new GridBagLayout();
	JScrollPane jScrollPaneProcIncoming = new JScrollPane();
	static JTextArea jTxtAreaProcIncming = new JTextArea();

	public AffiliateJobsImportFrame() {
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(gridBagLayout2);
		this.setDefaultCloseOperation(3);
		this.setSize(new Dimension(550, 511));
		this.setTitle("Test application. 2016");
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				this_windowOpened(e);
			}
		});

		lbl_POP3Error.setText("AFFILIATE JOBS - CHECKING MAIL ACTIVE");
		lbl_POP3Error.setForeground(colorActive);
		lbl_POP3Error.setFont(new java.awt.Font("Dialog", 1, 14));
		jPanelProcessIncoming.setLayout(gridBagLayout4);
		jTxtAreaProcIncming.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 11));
		jTxtAreaProcIncming.setText("Waiting ...");
		jTxtAreaProcIncming.setEditable(false);
		jPanelProcessIncoming.add(jScrollPaneProcIncoming,
															new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.BOTH,
																	new Insets(0, 0, 0, 0), 0, 0));
		jScrollPaneProcIncoming.getViewport().add(jTxtAreaProcIncming);
		contentPane.add(jPanelProcessIncoming,
										new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
												GridBagConstraints.BOTH,
												new Insets(0, 0, 0, 0), 0, 0));
	}

	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
			System.exit(0);
	}

	void this_windowOpened(WindowEvent e) {
		AccessRights.getInstance().setUserID(UserType.EMAIL);
		AffiliateJobsImportFrame.updateUI("My name is Email Checker. I try to help you.");
		AffiliateJobsImportFrame.updateUI("I will check your mailbox. Please, Wait a minute!");

		Thread thread = new Thread() {
			public void run() {

				while (true) {
					try {
					new UniversalMBox().run();

					AffiliateJobsImportFrame.updateUI("I finished checking your mailbox and I'm tired. I cannot find more new messages.");
					AffiliateJobsImportFrame.updateUI("I will check it again after 3 min!");

					
						Thread.sleep(180000L); // 3 min
					} catch (Exception e1) {
						System.err.println("Something wrong in main tgread. Err = " + e1.getMessage());
						AffiliateJobsImportFrame.updateUI("Something wrong in main tgread. Err = " + e1.getMessage());
						AffiliateJobsImportFrame.updateUI("I will try again after 3 minutes");
						// e1.printStackTrace();
					}
					AffiliateJobsImportFrame.updateUI("\n \n \n");
					AffiliateJobsImportFrame.updateUI("OK, I rested, and I will check again.");
				}
			}
		};
		thread.start();
	}

	public static void updateUI(String message) {
		String s = jTxtAreaProcIncming.getText();
		s = s + "\n";
		jTxtAreaProcIncming.setText(s + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date()) + ": " + message);
	}
}