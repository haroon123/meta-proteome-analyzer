package de.mpa.client.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

import de.mpa.analysis.UniProtUtilities;
import de.mpa.client.Client;
import de.mpa.client.Constants;
import de.mpa.client.ExportFields;
import de.mpa.client.model.AbstractExperiment;
import de.mpa.client.model.AbstractProject;
import de.mpa.client.model.DatabaseExperiment;
import de.mpa.client.model.DatabaseProject;
import de.mpa.client.model.dbsearch.DbSearchResult;
import de.mpa.client.model.dbsearch.MetaProteinFactory;
import de.mpa.client.settings.ResultParameters;
import de.mpa.client.ui.dialogs.AdvancedSettingsDialog;
import de.mpa.client.ui.dialogs.BlastDialog;
import de.mpa.client.ui.dialogs.ColorsDialog;
import de.mpa.client.ui.dialogs.ExportDialog;
import de.mpa.client.ui.dialogs.MetaproteinExportDialog;
import de.mpa.client.ui.dialogs.UpdateNcbiTaxDialog;
import de.mpa.client.ui.icons.IconConstants;
import de.mpa.db.accessor.ExpProperty;
import de.mpa.db.accessor.ExperimentAccessor;
import de.mpa.db.accessor.ProjectAccessor;
import de.mpa.db.accessor.Property;
import de.mpa.db.accessor.ProteinAccessor;

/**
 * The main application frame's menu bar.
 * 
 * @author A. Behne
 */
public class ClientFrameMenuBar extends JMenuBar {
	
	/**
	 * The client frame instance.
	 */
	private ClientFrame clientFrame;
	
	/**
	 * Class containing all values for the export checkboxes.
	 */
	private ExportFields exportFields;

	/**
	 * The 'Export' menu.
	 */
	private JMenu exportMenu;
	
	/**
	 * Constructs the client frame menu bar and initializes the components.
	 * @param clientFrame The client frame. 
	 */
	public ClientFrameMenuBar() {
		this.clientFrame = ClientFrame.getInstance();
		Client.getInstance();
		exportFields = ExportFields.getInstance();
		initComponents();
	}
	
	/**
	 * Initializes the components.
	 */
	private void initComponents() {
		this.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.SINGLE);

		/* create File Menu */
		JMenu fileMenu = new JMenu();
		fileMenu.setText("File");

		// create Exit item
		JMenuItem exitItem = new JMenuItem();
		exitItem.setText("Exit");
		exitItem.setIcon(IconConstants.EXIT_ICON);
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Client.exit();
			}
		});
		
		fileMenu.add(exitItem);

		/* create Settings menu */
		JMenu settingsMenu = new JMenu("Settings");
		
		// create Color Settings item
		JMenuItem colorsItem = new JMenuItem("Color Settings", IconConstants.COLOR_SETTINGS_ICON);
		colorsItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				ColorsDialog.getInstance().setVisible(true);
			}
		});

		// create Connection Settings item
		JMenuItem connItem = new JMenuItem("Connection Settings", IconConstants.CONNECT_SMALL_ICON);
		connItem.setEnabled(!Client.isViewer());
		connItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				AdvancedSettingsDialog.showDialog(clientFrame, "Connection Settings", true, Client.getInstance().getConnectionParameters());
			}
		});

		settingsMenu.add(colorsItem);
		settingsMenu.addSeparator();
		settingsMenu.add(connItem);

		/* create Export menu */
		exportMenu = new JMenu("Export");
		
		JMenuItem mpaItem = new JMenuItem("MPA File...", IconConstants.MPA_SMALL_ICON);
		mpaItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				exportMPA();
			}
		});
		
		// Export CSV results
		JMenuItem csvItem = new JMenuItem("CSV File...", IconConstants.EXCEL_EXPORT_ICON);
		csvItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				exportCSV();
			}
		});

		// Export graphML file
		JMenuItem graphmlItem = new JMenuItem("GraphML File...", IconConstants.GRAPH_ICON);
		graphmlItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				exportGraphML();
			}
		});
		
		// Export Meta-proteins Menu Item
		JMenuItem metaProteinsExportItem = new JMenuItem("Meta-Proteins...");

		metaProteinsExportItem.setIcon(new ImageIcon(getClass().getResource("/de/mpa/resources/icons/excel_export16.png")));

		metaProteinsExportItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					openMetaproteinExportDialog();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		});
		

		exportMenu.add(mpaItem);
		exportMenu.addSeparator();
		exportMenu.add(csvItem);	
		exportMenu.add(metaProteinsExportItem);
		exportMenu.addSeparator();
		exportMenu.add(graphmlItem);
		
		this.setExportMenuEnabled(false);
		

		// Update Menu
		JMenu updateMenu = new JMenu();		
		updateMenu.setText("Update");
		
		// Update contents

		// fill in information for UniProt 100 90 50 etc. References
		JMenuItem updateUniProtItem = new JMenuItem();
		updateUniProtItem.setText("Update UniRef's");
		updateUniProtItem.setIcon(new ImageIcon(getClass().getResource("/de/mpa/resources/icons/uniprot16.png")));
		updateUniProtItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SwingWorker<Object, Object>() {
					@Override
					protected Object doInBackground() throws SQLException {
						UniProtUtilities.repairMissingUniRefs();
						return null;
					}
				}.execute();
			}
		});
		// Find unreferenced UniProt entries in the database and try to fill them
		JMenuItem updateEmptyUniProtItem = new JMenuItem();
		updateEmptyUniProtItem.setText("Update empty UniProt Entries");
		updateEmptyUniProtItem.setIcon(new ImageIcon(getClass().getResource("/de/mpa/resources/icons/uniprot16.png")));
		updateEmptyUniProtItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SwingWorker<Object, Object>() {
					@Override
					protected Object doInBackground() throws SQLException  {
						// find all proteins in the database and pass them on
						Map<String, Long> protMap = ProteinAccessor.findAllProteins(Client.getInstance().getConnection());
						Set<Long> proteins = new HashSet<Long>();
						for (long id : protMap.values()) {
							proteins.add(id);
						}
						UniProtUtilities.updateUniProtEntries(proteins, null, null, 0, false);
						return null;
					}	
				}.execute();
			}
		});
		// Find unreferenced UniProt entries in the database and try to BLAST them if necessary
		JMenuItem blastItem = new JMenuItem();
		blastItem.setText("BLAST unknown Hits");
		blastItem.setIcon(new ImageIcon(getClass().getResource("/de/mpa/resources/icons/blast16.png")));
		blastItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new BlastDialog(clientFrame, "Blast_Dialog");
			}
		});
		// Add items to update menu
		updateMenu.add(updateUniProtItem);
		updateMenu.add(updateEmptyUniProtItem);
		updateMenu.add(blastItem);
		updateMenu.addSeparator();
		// Help Menu
		JMenuItem updateNcbiTaxItem = new JMenuItem();		
		updateNcbiTaxItem.setText("Update NCBI Taxonomy");
		updateNcbiTaxItem.setIcon(new ImageIcon(getClass().getResource("/de/mpa/resources/icons/ncbi16.png")));
		updateNcbiTaxItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new UpdateNcbiTaxDialog(clientFrame, "Update NCBI-Taxonomy Dialog");
			}
		});
		updateMenu.add(updateNcbiTaxItem);
				
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
				showHelp();
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
		// Add only for the client and not for the viewer
		if (!Client.isViewer()) {
			this.add(updateMenu);
		}
		this.add(helpMenu);
	}
	
	/**
	 * Method to open the metaprotein export dialog
	 * @throws SQLException 
	 */
	protected void openMetaproteinExportDialog() throws SQLException {
		new MetaproteinExportDialog(clientFrame, "User Export Dialog");
	}

	/**
	 * Executed when the save project button is triggered. Via a file chooser the user can select the destination of the project (MPA) file.
	 */
	private void exportMPA() {
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
						Client.getInstance().exportDatabaseSearchResult(filePath);
					}
				}
				return null;
			}
		}.execute();
	}

	/**
	 * This method opens the export dialog.
	 */
    private void exportCSV() {
    	new ExportDialog(clientFrame, "Results Export", true, exportFields);
//    	AdvancedSettingsDialog.showDialog(clientFrame, "Export Results to CSV", true, new ResultExportParameters());
    }
    
    /**
     * Executed when the graphML menu item is triggered. Via a file chooser the user can select the destination of the GraphML file.
     */
	private void exportGraphML() {
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
	 * This method is being executed when the help menu item is selected.
	 */
	private void showHelp() {
		new HtmlFrame(clientFrame, getClass().getResource("/de/mpa/resources/html/help.html"), "Help");
	}

	/**
	 * The method that builds the about dialog.
	 */
	private void showAbout() {
		StringBuffer tMsg = new StringBuffer();
		tMsg.append("Product Version: " + Constants.APPTITLE + " " + Constants.VER_NUMBER);
		tMsg.append("\n\n");
		tMsg.append("This software is developed by Alexander Behne, Robert Heyer, Fabian Kohrs and Thilo Muth \nat the Otto von Guericke University Magdeburg and the Max Planck Institute for Dynamics of Complex \nTechnical Systems in Magdeburg (Germany).");
		tMsg.append("\n\n");
		tMsg.append("The latest version is available at http://meta-proteome-analyzer.googlecode.com");
		tMsg.append("\n\n");
		tMsg.append("If any questions arise, contact the corresponding author: ");
		tMsg.append("\n");
		tMsg.append("muth@mpi-magdeburg.mpg.de");
		tMsg.append("\n\n");
		JOptionPane.showMessageDialog(this, tMsg,
				"About " + Constants.APPTITLE, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Sets the enable state of the 'Export' menu.
	 * @param enabled <code>true</code> if enabled, <code>false</code> otherwise
	 */
	public void setExportMenuEnabled(boolean enabled) {
		for (Component comp : exportMenu.getMenuComponents()) {
			comp.setEnabled(enabled);
		}
	}
    
}
