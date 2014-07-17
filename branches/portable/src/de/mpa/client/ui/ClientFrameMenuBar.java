package de.mpa.client.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.error.ErrorLevel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

import de.mpa.client.Client;
import de.mpa.client.Constants;
import de.mpa.client.ExportFields;
import de.mpa.client.settings.ServerConnectionSettings;
import de.mpa.client.ui.dialogs.ColorsDialog;
import de.mpa.client.ui.dialogs.ExportDialog;
import de.mpa.client.ui.icons.IconConstants;
import de.mpa.db.DbConnectionSettings;

public class ClientFrameMenuBar extends JMenuBar {
	/**
	 * Client frame instance.
	 */
	private ClientFrame clientFrame;
	
	/**
	 * Client instance.
	 */
	private Client client;
	private JTextField srvHostTtf;
	private JTextField srvPortTtf;
	private JTextField dbDriverTtf;
	private JTextField dbLocalUrlTtf;
	private JTextField dbRemoteUrlTtf;
	private JTextField dbPortTtf;
	private JTextField dbUserTtf;
	private JPasswordField dbPassTtf;
	private JLabel dbConnTestLbl;
	private JPanel dbPnl;
	private JMenuItem exportCSVResultsItem;
	private JMenuItem saveProjectItem;
	private JMenuItem exportGraphMLItem;

	
	/**
	 * Class containing all values for the export checkboxes.
	 */
	private ExportFields exportFields;
	
	/**
	 * Constructs the client frame menu bar and initializes the components.
	 * @param clientFrame The client frame. 
	 */
	public ClientFrameMenuBar() {
		this.clientFrame = ClientFrame.getInstance();
		this.client = Client.getInstance();
		exportFields = ExportFields.getInstance();
		initComponents();
	}
	
	/**
	 * Initializes the components.
	 */
	private void initComponents() {
		this.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.SINGLE);

		// File Menu
		JMenu fileMenu = new JMenu();
		fileMenu.setText("File");
		JMenuItem newProjectItem = new JMenuItem();
		newProjectItem.setText("New Project");
		newProjectItem.setIcon(IconConstants.ADD_FOLDER_ICON);
		newProjectItem.setEnabled(false);

		JMenuItem openProjectItem = new JMenuItem();
		openProjectItem.setText("Open Project");
		openProjectItem.setIcon(IconConstants.VIEW_FOLDER_ICON);
		openProjectItem.setEnabled(false);
		
		saveProjectItem = new JMenuItem();
		saveProjectItem.setText("Save Project");
		saveProjectItem.setIcon(IconConstants.SAVE_ICON);
		saveProjectItem.setEnabled(false);
		saveProjectItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				saveProjectButtonTriggered();
			}
		});

		// exitItem
		JMenuItem exitItem = new JMenuItem();
		exitItem.setText("Exit");
		exitItem.setIcon(IconConstants.EXIT_ICON);
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Client.exit();
			}
		});
		
		fileMenu.add(newProjectItem);
		fileMenu.add(openProjectItem);
		fileMenu.add(saveProjectItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);

		// Settings Menu
		JMenu settingsMenu = new JMenu("Settings");
		
		// Color settings item
		JMenuItem colorsItem = new JMenuItem("Color Settings", IconConstants.COLOR_SETTINGS_ICON);
		colorsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ColorsDialog.getInstance().setVisible(true);
			}
		});

		// Database settings item
		JMenuItem databaseItem = new JMenuItem("Database Connection", 
				IconConstants.DATABASE_SETTINGS_ICON);
		databaseItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showDatabaseSettings();
			}
		});
		databaseItem.setEnabled(!Client.isViewer());

		// Server settings item
		JMenuItem serverItem = new JMenuItem("Server Configuration",
				IconConstants.SERVER_SETTINGS_ICON);
		final JPanel srvPnl = constructServerSettingsPanel();
		serverItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ServerConnectionSettings oldSrvSettings = client.getServerConnectionSettings();
				int res = JOptionPane.showConfirmDialog(clientFrame, srvPnl, "Server Configuration",
						  JOptionPane.OK_CANCEL_OPTION,
						  JOptionPane.PLAIN_MESSAGE);
				if (res == JOptionPane.OK_OPTION) {
					client.setServerConnectionSettings(gatherServerSettings());
				} else {
					client.setServerConnectionSettings(oldSrvSettings);
				}
			}
		});
		serverItem.setEnabled(!Client.isViewer());

		settingsMenu.add(colorsItem);
		settingsMenu.addSeparator();
		settingsMenu.add(databaseItem);
		settingsMenu.add(serverItem);

		// Export menu
		JMenu exportMenu = new JMenu();
		exportMenu.setText("Export");
		// Export CSV results
		exportCSVResultsItem = new JMenuItem();
		exportCSVResultsItem.setText("CSV Results");
		exportCSVResultsItem.setIcon(IconConstants.EXCEL_EXPORT_ICON);
		exportCSVResultsItem.setEnabled(false);
		exportCSVResultsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				showExportDialog();
			}
		});
		exportMenu.add(exportCSVResultsItem);	

		exportMenu.addSeparator();

		// Export graphML file
		exportGraphMLItem = new JMenuItem();
		exportGraphMLItem.setText("GraphML File");
		exportGraphMLItem.setIcon(IconConstants.GRAPH_ICON);
		exportGraphMLItem.setEnabled(false);
		exportGraphMLItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				saveGraphMLItemTriggered();
			}
		});
		exportMenu.add(exportGraphMLItem);	
		
		
		// Help Menu
		JMenu helpMenu = new JMenu();		
		helpMenu.setText("Help");

		// Help Contents
		JMenuItem helpContentsItem = new JMenuItem();
		helpContentsItem.setText("Help Contents");

		helpContentsItem.setIcon(new ImageIcon(getClass().getResource("/de/mpa/resources/icons/help.gif")));
		helpMenu.add(helpContentsItem);
		helpContentsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				helpTriggered();
			}
		});
		helpMenu.addSeparator();

		// aboutItem
		JMenuItem aboutItem = new JMenuItem();
		aboutItem.setText("About");
		aboutItem.setIcon(new ImageIcon(getClass().getResource("/de/mpa/resources/icons/about.gif")));
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showAbout();
			}
		});
		helpMenu.add(aboutItem);
		
		this.add(fileMenu);
		this.add(settingsMenu);
		this.add(exportMenu);
		this.add(helpMenu);
	}
	
	/**
	 * This method is being executed when the help menu item is selected.
	 */
	private void helpTriggered() {
		new HtmlFrame(clientFrame, getClass().getResource("/de/mpa/resources/html/help.html"), "Help");
	}
	
	/**
	 * Shows the database settings.
	 */
	public void showDatabaseSettings() {
		if (dbPnl == null) {
			dbPnl = constructDbSettingsPanel();
		}
		
		DbConnectionSettings oldDbSettings = client.getDatabaseConnectionSettings();
		int res = JOptionPane.showConfirmDialog(clientFrame, dbPnl, "Database Settings", 
				  JOptionPane.OK_CANCEL_OPTION,
				  JOptionPane.PLAIN_MESSAGE);
		if (res == JOptionPane.OK_OPTION) {
			client.setDatabaseConnectionSettings(gatherDbSettings());	// update settings
		} else {	// cancel option or window close option
			client.setDatabaseConnectionSettings(oldDbSettings);	// revert to old settings
		}
	}

	/**
	 * The method that builds the about dialog.
	 */
	private void showAbout() {
		StringBuffer tMsg = new StringBuffer();
		tMsg.append("Product Version: " + Constants.APPTITLE + " " + Constants.VER_NUMBER);
		tMsg.append("\n");
		tMsg.append("\n");
		tMsg.append("This software is developed by Alexander Behne, Robert Heyer and Thilo Muth \nat the Max Planck Institute for Dynamics of Complex \nTechnical Systems in Magdeburg (Germany).");
		tMsg.append("\n");
		tMsg.append("\n");
		tMsg.append("The latest version is available at http://meta-proteome-analyzer.googlecode.com");
		tMsg.append("\n");
		tMsg.append("\n");
		tMsg.append("If any questions arise, contact the corresponding author: ");
		tMsg.append("\n");
		tMsg.append("muth@mpi-magdeburg.mpg.de");
		tMsg.append("\n");
		tMsg.append("\n");
		tMsg.append("");
		tMsg.append("");
		JOptionPane.showMessageDialog(this, tMsg,
				"About " + Constants.APPTITLE, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Method to gather database settings from GUI elements.
	 * @return Database connections settings
	 */
	private DbConnectionSettings gatherDbSettings() {
		DbConnectionSettings dbSettings = new DbConnectionSettings();
		dbSettings.setJdbcDriver(dbDriverTtf.getText());
		dbSettings.setUrlLocale(dbLocalUrlTtf.getText());
		dbSettings.setUrlRemote(dbRemoteUrlTtf.getText());
		dbSettings.setPort(dbPortTtf.getText());
		dbSettings.setUsername(dbUserTtf.getText());
		dbSettings.setPassword(new String(dbPassTtf.getPassword()));
		return dbSettings;
	}

	/**
	 * Method to construct database settings panel.
	 * @return
	 */
	private JPanel constructDbSettingsPanel() {
		
		final JPanel dbPnl = new JPanel();
		
		CellConstraints cc = new CellConstraints();
		
		dbPnl.setLayout(new FormLayout("5dlu, p, 5dlu, p, 5dlu  ",
				"5dlu, p, 5dlu, t:p, 5dlu, p, 5dlu, t:p, 5dlu, p, 5dlu, t:p, 5dlu, t:p, 5dlu"));
		dbPnl.setBorder(new TitledBorder("Database"));
		dbPnl.add(new JLabel("JDBC driver"),cc.xy(2,2));
		dbPnl.add(new JLabel("URL locale"), cc.xy(2,4));
		dbPnl.add(new JLabel("URL remote"), cc.xy(2,6));
		dbPnl.add(new JLabel("Port"), cc.xy(2,8));
		dbPnl.add(new JLabel("Username"), cc.xy(2,10));
		dbPnl.add(new JLabel("Password"), cc.xy(2,12));

		final DbConnectionSettings dbSettings = client.getDatabaseConnectionSettings();

		dbDriverTtf = new JTextField(dbSettings.getJdbcDriver());
		dbPnl.add(dbDriverTtf,cc.xy(4, 2));
		dbLocalUrlTtf = new JTextField(dbSettings.getUrlLocale());
		dbPnl.add(dbLocalUrlTtf,cc.xy(4, 4));
		dbRemoteUrlTtf = new JTextField(dbSettings.getUrlRemote());
		dbPnl.add(dbRemoteUrlTtf,cc.xy(4, 6));
		dbPortTtf = new JTextField(dbSettings.getPort());
		dbPnl.add(dbPortTtf,cc.xy(4, 8));
		dbUserTtf = new JTextField(dbSettings.getUsername());
		dbPnl.add(dbUserTtf,cc.xy(4, 10));
		dbPassTtf = new JPasswordField(dbSettings.getPassword());
		dbPnl.add(dbPassTtf,cc.xy(4, 12));
		dbConnTestLbl = new JLabel("");
		dbPnl.add(dbConnTestLbl,cc.xy(4, 14));
		JButton testConnBtn = new JButton("Test connection");
		// action listener for button "Test connection"
		testConnBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				DbConnectionSettings dbSettings = gatherDbSettings();
				client.setDatabaseConnectionSettings(dbSettings);

				// method closes old connection
				try {
					client.closeDBConnection();				
				} catch (Exception e) {
					e.printStackTrace();
				}
				// try new connection				
				try {
					client.getConnection();
					dbConnTestLbl.setText("Connection OK");
					dbConnTestLbl.setForeground(Color.GREEN);
				} catch (Exception e) {
					dbConnTestLbl.setText("Connection failed");
					dbConnTestLbl.setForeground(Color.RED);
				}
			}
		});

		dbPnl.add(testConnBtn,cc.xy(2, 14));
		return dbPnl;
	}

	/**
	 * Method to gather server settings from GUI elements.
	 * @return
	 */
	private ServerConnectionSettings gatherServerSettings() {

		ServerConnectionSettings srvSettings = new ServerConnectionSettings();
		
		srvSettings.setHost(srvHostTtf.getText());
		srvSettings.setPort(srvPortTtf.getText());
		
		return srvSettings;
	}
	
	/**
	 * Method to construct server settings panel.
	 * @return
	 */
	private JPanel constructServerSettingsPanel() {
		JPanel srvPnl = new JPanel();
		CellConstraints cc = new CellConstraints();
		srvPnl.setLayout(new FormLayout("5dlu, p, 5dlu, p:g, 5dlu", "p, 3dlu, p, 3dlu, p, 5dlu"));
		srvPnl.setBorder(new TitledBorder("Server Address"));

		srvHostTtf = new JTextField(8);
		srvHostTtf.setText(ServerConnectionSettings.DEFAULT_HOST);
		srvPortTtf = new JTextField(8);
		srvPortTtf.setText(ServerConnectionSettings.DEFAULT_PORT);

		srvPnl.add(new JLabel("Hostname:"), cc.xy(2,1));
		srvPnl.add(srvHostTtf, cc.xy(4,1));
		srvPnl.add(new JLabel("Port:"), cc.xy(2,3));
		srvPnl.add(srvPortTtf, cc.xy(4,3));

		JButton connectBtn = new JButton("Connect to server");	    
		connectBtn.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					client.connect();
					clientFrame.setServerConnection(true);
					clientFrame.getFilePanel().getSettingsPanel().getProcessButton().setEnabled(true);
					JOptionPane.showMessageDialog(clientFrame, "Web Service @" + srvHostTtf.getText() + ":" + srvPortTtf.getText() + " established.");
				} catch (Exception e) {
					JXErrorPane.showDialog(ClientFrame.getInstance(),
							new ErrorInfo("Severe Error", "<html>Could not establish connection to specified server.<br>Please check your configuration and/or try again later.</html>", null, null, e, ErrorLevel.SEVERE, null));
				}
			}
		});
		srvPnl.add(connectBtn, cc.xyw(2,5,3));
		return srvPnl;
	}
	
	/**
	 * This method opens the export dialog.
	 */
    private void showExportDialog() {
    	new ExportDialog(clientFrame, "Results Export", true, exportFields);
    }
    
    /**
     * Executed when the save project button is triggered. Via a file chooser the user can select the destination of the project (MPA) file.
     */
	private void saveProjectButtonTriggered() {
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				JFileChooser chooser = new ConfirmFileChooser();
				chooser.setCurrentDirectory(new File(clientFrame.getLastSelectedFolder()));
				chooser.setFileFilter(Constants.MPA_FILE_FILTER);
				chooser.setAcceptAllFileFilterUsed(false);
				int returnVal = chooser.showSaveDialog(clientFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File selFile = chooser.getSelectedFile();
					if (selFile != null) {
						String filePath = selFile.getPath();
						clientFrame.setLastSelectedFolder(selFile.getParent());
						if (!filePath.toLowerCase().endsWith(".mpa")) {
							filePath += ".mpa";
						}
						Client.getInstance().writeDbSearchResultToFile(filePath);
					}
				}
				return null;
			}
		}.execute();
	}
	
    /**
     * Executed when the graphML menu item is triggered. Via a file chooser the user can select the destination of the GraphML file.
     */
	private void saveGraphMLItemTriggered() {
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				JFileChooser chooser = new ConfirmFileChooser();
				chooser.setFileFilter(Constants.GRAPHML_FILE_FILTER);
				chooser.setAcceptAllFileFilterUsed(false);
				int returnVal = chooser.showSaveDialog(clientFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File selFile = chooser.getSelectedFile();
					if (selFile != null) {
						String filePath = selFile.getPath();
						if (!filePath.toLowerCase().endsWith(".graphml")) {
							filePath += ".graphml";
						}
						Client.getInstance().getGraphDatabaseHandler().exportGraph(new File(filePath));
					}
				}
				return null;
			}
		}.execute();
	}
    
    /**
     * Enables the export CSV results function.
     * @param enabled The state of the CSV result export menu item
     */
	public void setExportCSVResultsEnabled(boolean enabled) {
    	exportCSVResultsItem.setEnabled(enabled);
    }
	
    /**
     * Enables the export GraphML function.
     * @param enabled The state of the GraphML export menu item
     */
	public void setExportGraphMLEnabled(boolean enabled) {
    	exportGraphMLItem.setEnabled(enabled);
    }
    
    /**
     * Enables the save project function.
     * @param enabled The state of the save project menu item
     */
	public void setSaveProjectEnabled(boolean enabled) {
    	saveProjectItem.setEnabled(enabled);
    }
}