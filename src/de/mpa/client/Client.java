package de.mpa.client;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.log4j.Logger;

import de.mpa.algorithms.Interval;
import de.mpa.algorithms.RankedLibrarySpectrum;
import de.mpa.client.model.DenovoSearchResult;
import de.mpa.client.model.ExperimentContent;
import de.mpa.client.model.ExperimentResult;
import de.mpa.client.model.PeptideHit;
import de.mpa.client.model.PeptideSpectrumMatch;
import de.mpa.client.model.ProjectContent;
import de.mpa.client.model.ProteinHit;
import de.mpa.client.ui.CheckBoxTreeManager;
import de.mpa.client.ui.CheckBoxTreeSelectionModel;
import de.mpa.client.ui.SpectrumTree;
import de.mpa.db.ConnectionType;
import de.mpa.db.DBConfiguration;
import de.mpa.db.DbConnectionSettings;
import de.mpa.db.accessor.Cruxhit;
import de.mpa.db.accessor.Inspecthit;
import de.mpa.db.accessor.Omssahit;
import de.mpa.db.accessor.Pepnovohit;
import de.mpa.db.accessor.PeptideAccessor;
import de.mpa.db.accessor.ProteinAccessor;
import de.mpa.db.accessor.SearchHit;
import de.mpa.db.accessor.Searchspectrum;
import de.mpa.db.accessor.Spectrum;
import de.mpa.db.accessor.XTandemhit;
import de.mpa.db.extractor.SpectralSearchCandidate;
import de.mpa.db.extractor.SpectrumExtractor;
import de.mpa.io.MascotGenericFile;
import de.mpa.io.MascotGenericFileReader;
import de.mpa.webservice.WSPublisher;

public class Client {

	// Client instance
	private static Client client = null;
	
	// Server service
	private ServerImplService service;
	
	// Logger
	private Logger log = Logger.getLogger(getClass());
	
	// Server instance
	private Server server;
	
	// Connection
	private Connection conn;

	private DbConnectionSettings dbSettings = new DbConnectionSettings();
	private ServerConnectionSettings srvSettings = new ServerConnectionSettings();

	// TODO: move methods
	public DbConnectionSettings getDbSettings() {
		return dbSettings;
	}

	public void setDbSettings(DbConnectionSettings dbSettings) {
		this.dbSettings = dbSettings;
	}
	
	public ServerConnectionSettings getServerSettings() {
		return srvSettings;
	}

	public void setServerSettings(ServerConnectionSettings srvSettings) {
		this.srvSettings = srvSettings;
	}

	//
	/**
     *  Property change support for notifying the gui about new messages.
     */
    private PropertyChangeSupport pSupport;

	private ExperimentResult experimentResult;

	/**
	 * The constructor for the client (private for singleton object).
	 * 
	 * @param name
	 */
	private Client() {
		pSupport = new PropertyChangeSupport(this);
	}
	
	/**
	 * Returns a client singleton object.
	 * 
	 * @return client Client singleton object
	 */
	public static Client getInstance() {
		if (client == null) {
			client = new Client();
		}
		return client;
	}
	
	
	//End SQL for protein Databases****************************************************
	/**
	 * Sets the database connection.
	 * @throws SQLException 
	 */
	public void initDBConnection() throws SQLException {
		// Connection conn
		if (conn == null) {
			// connect to database
			DBConfiguration dbconfig = new DBConfiguration("metaprot", ConnectionType.REMOTE, this.dbSettings);
			this.conn = dbconfig.getConnection();
		}
	}
	
	/**
	 * Clears the database connection.
	 * @throws SQLException 
	 */
	public void closeDBConnection() throws SQLException {
		if (conn != null) {
			conn.close();
			conn = null;
		}
	}

	/**
	 * Connects the client to the web service.
	 */
	public void connect() {
		
		WSPublisher.start(srvSettings.getHost(), srvSettings.getPort());
		
		service = new ServerImplService();
		server = service.getServerImplPort();
		
		// enable MTOM in client
		BindingProvider bp = (BindingProvider) server;
		SOAPBinding binding = (SOAPBinding) bp.getBinding();
		binding.setMTOMEnabled(true);
		
		// Start requesting
		RequestThread thread = new RequestThread();
		thread.start();
	}
	
	/**
	 * Requests the server for response.
	 */
	public void request(){
		final String message = receiveMessage();
		if(message != null && !message.equals("")){
			log.info(message);
			EventQueue.invokeLater(new Runnable() {                                                 
				public void run() {
					pSupport.firePropertyChange("New Message", null, message);                                                    
				}
			});
		}
	}
	
	/**
	 * Send the message. 
	 * @param msg
	 */
	public String receiveMessage(){
		return server.sendMessage();
	}
	
	/**
	 * Send multiple files to the server.
	 * @param files
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public void sendFiles(List<File> files) throws FileNotFoundException, IOException {		
		// Send files iteratively
		for (int i = 0; i < files.size(); i++){			
			server.uploadFile(files.get(i).getName(), getBytesFromFile(files.get(i)));
		}
	}
	
	/**
	 * Returns the contents of the file in a byte array.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private byte[] getBytesFromFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);

	    // Get the size of the file
	    long length = file.length();

	    // Before converting to an int type, check to ensure that file is not larger than Integer.MAX_VALUE.
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    	throw new IOException("File size too long: " + length);
	    }

	    // Create the byte array to hold the data
	    byte[] bytes = new byte[(int)length];

	    // Read in the bytes
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }

	    // Ensure all the bytes have been read in
	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file " + file.getName());
	    }

	    // Close the input stream and return bytes
	    is.close();
	    return bytes;
	}
	
	/**
	 * Runs the database search.
	 * @param file
	 */
	public void runDbSearch(List<File> files, DbSearchSettings settings){
		// Iterate the files
		for (int i = 0; i < files.size(); i++) {
			server.runDbSearch(files.get(i).getName(), settings);
		}
	}
	

	
	/**
	 * Runs the de-novo search.
	 * @param file
	 */
	public void runDenovoSearch(List<File> files, DenovoSearchSettings settings){
		// Iterate the files
		for (int i = 0; i < files.size(); i++){				
			server.runDenovoSearch(files.get(i).getName(), settings);
		}
	}
	
	/**
	 * Returns the result from the de-novo search.
	 * @param file The query file.
	 * @return DenovoSearchResult
	 */
	public DenovoSearchResult getDenovoSearchResult(File file){
		// Initialize the connection.
		try {
			initDBConnection();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		DenovoSearchResult result = null;
		
		MascotGenericFileReader mgfReader;
		List<MascotGenericFile> mgfFiles = null;
		try {
			// Get the query spectra.
			mgfReader = new MascotGenericFileReader(file);
			mgfFiles = mgfReader.getSpectrumFiles();
			
			// Initialize the result set.
			result = new DenovoSearchResult();
			List<Spectrum> querySpectra = new ArrayList<Spectrum>();
			Map<String, List<Pepnovohit>> pepnovoResults = new HashMap<String, List<Pepnovohit>>();
			
			// Iterate over query spectra and get the different identification result sets
			for (MascotGenericFile mgf : mgfFiles) {
				Spectrum spectrum = Spectrum.findFromTitle(mgf.getTitle(), conn);
				querySpectra.add(spectrum);
				long spectrumID = spectrum.getSpectrumid();
				String spectrumname = spectrum.getTitle();
				
				// Pepnovo
				List<Pepnovohit> pepnovoList = Pepnovohit.getHitsFromSpectrumID(spectrumID, conn);
				if(pepnovoList.size() > 0) {
					pepnovoResults.put(spectrumname, pepnovoList);
				}
				
			}
			
			// Set the results.
			result.setQuerySpectra(querySpectra);
			result.setPepnovoResults(pepnovoResults);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Returns the result(s) from the database search for a particular experiment.
	 * @param experimentid The experiment id
	 * @return DbSearchResult
	 */
	public void retrieveExperimentResult(ProjectContent projContent,ExperimentContent expContent){
		// Init the database connection.
		try {
			initDBConnection();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		try {			
			// The protein hit set, containing all information about found proteins.
			experimentResult = new ExperimentResult(projContent.getProjectTitle(), expContent.getExperimentTitle(),  "EASTER EGG");
			
			// Iterate over query spectra and get the different identification result sets
			List<Searchspectrum> searchSpectra = Searchspectrum.findFromExperimentID(expContent.getExperimentID(), conn);
			
			//TODO: Get search date from run table
			Date searchDate = null;
			for (Searchspectrum searchSpectrum : searchSpectra) {
				
				long searchSpectrumId = searchSpectrum.getSearchspectrumid();
				
				// X!Tandem
				List<XTandemhit> xtandemList = XTandemhit.getHitsFromSpectrumID(searchSpectrumId, conn);
				if(xtandemList.size() > 0) {
					for (XTandemhit hit : xtandemList) {
						addProteinSearchHit(hit);
					}
					
					// Set creation date
					if(searchDate == null){
						experimentResult.setSearchDate(xtandemList.get(0).getCreationdate());
					}
				}
				
				// Omssa
				List<Omssahit> omssaList = Omssahit.getHitsFromSpectrumID(searchSpectrumId, conn);
				if(omssaList.size() > 0) {
					for (Omssahit hit : omssaList) {
						addProteinSearchHit(hit);
					}
				}
				// Crux
				List<Cruxhit> cruxList = Cruxhit.getHitsFromSpectrumID(searchSpectrumId, conn);				
				if(cruxList.size() > 0) {
					//cruxResults.put(spectrumname, cruxList);
//					for (Cruxhit hit : cruxList) {
//						// TODO: addProteinSearchHit(hit);
//					}
				}
				// Inspect
				List<Inspecthit> inspectList = Inspecthit.getHitsFromSpectrumID(searchSpectrumId, conn);		
				if(inspectList.size() > 0) {
					//inspectResults.put(spectrumname, inspectList);
					for (Inspecthit hit : inspectList) {
						addProteinSearchHit(hit);
					}
				}
			}
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the current experiment result.
	 * @param projContent 
	 * @param expContent
	 * @return
	 */
	public ExperimentResult getExperimentResult(ProjectContent projContent,ExperimentContent expContent) {
		if(experimentResult == null) {
			retrieveExperimentResult(projContent, expContent);
		}
		return experimentResult;
	}
	
	/**
	 * Returns the current experiment result.
	 * @return experimentResult The current experiment result.
	 */
	public ExperimentResult getExperimentResult() {
		return experimentResult;
	}

	/**
	 * This method converts a search hit into a protein hit and adds it to the current protein hit set.
	 * @param hit The search hit implementation.
	 * @throws SQLException when the retrieval did not succeed.
	 */
	private void addProteinSearchHit(SearchHit hit) throws SQLException{
		
		// Create the PeptideSpectrumMatch
		PeptideSpectrumMatch psm = new PeptideSpectrumMatch(hit.getFk_searchspectrumid(), hit);
		
		// Get the peptide hit.
		PeptideAccessor peptide = PeptideAccessor.findFromID(hit.getFk_peptideid(), conn);
		PeptideHit peptideHit = new PeptideHit(peptide.getSequence(), psm);
		
		// Get the protein accessor.
		ProteinAccessor protein = ProteinAccessor.findFromID(hit.getFk_proteinid(), conn);
		
		// Add a new protein to the protein hit set.
		experimentResult.addProtein(new ProteinHit(protein.getAccession(), protein.getDescription(), peptideHit));
	}
	
	/**
	 * TODO: API!
	 * @param experimentID
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<SpectralSearchCandidate> getCandidatesFromExperiment(long experimentID) throws SQLException {
		initDBConnection();
		return new SpectrumExtractor(conn).getCandidatesFromExperiment(experimentID);
	}
	
	/**
	 * TODO: API!
	 * @param file
	 * @param procSet
	 * @param processWorker 
	 * @return resultMap
	 */
	public HashMap<String, ArrayList<RankedLibrarySpectrum>> searchSpecLib(File file, SpecSimSettings procSet) {
		// declare result map
		HashMap<String, ArrayList<RankedLibrarySpectrum>> resultMap = null;
		
		try {
			// parse query file
			MascotGenericFileReader mgfReader = new MascotGenericFileReader(file);
			List<MascotGenericFile> mgfFiles = mgfReader.getSpectrumFiles();
			
			// store list of results in HashMap (with spectrum title as key)
			resultMap = new HashMap<String, ArrayList<RankedLibrarySpectrum>>(mgfFiles.size());
			
			// iterate query spectra to gather precursor m/z values
			ArrayList<Double> precursorMZs = new ArrayList<Double>(mgfFiles.size());
			for (MascotGenericFile mgf : mgfFiles) {
				precursorMZs.add(mgf.getPrecursorMZ());
			}
			Collections.sort(precursorMZs);
			// build list of precursor m/z intervals using sorted list
			ArrayList<Interval> intervals = new ArrayList<Interval>();
			Interval current = null;
			for (double precursorMz : precursorMZs) {
				if (current == null) {	// first interval
					current = new Interval(((precursorMz - procSet.getTolMz()) < 0.0) ? 0.0 : precursorMz - procSet.getTolMz(), precursorMz + procSet.getTolMz());
					intervals.add(current);
				} else {
					// if left border of new interval intersects current interval extend the latter
					if ((precursorMz - procSet.getTolMz()) < current.getRightBorder()) {
						current.setRightBorder(precursorMz + procSet.getTolMz());
					} else {	// generate new interval
						current = new Interval(precursorMz - procSet.getTolMz(), precursorMz + procSet.getTolMz());
						intervals.add(current);
					}
				}
			}

			// extract list of candidates
			SpectrumExtractor specEx = new SpectrumExtractor(conn);
			ArrayList<SpectralSearchCandidate> candidates = 
				specEx.getCandidatesFromExperiment(intervals, procSet.getExperimentID());
			
			// iterate query spectra to determine similarity scores
//			int progress = 0;
			for (MascotGenericFile mgfQuery : mgfFiles) {
				
				// store results in list of ranked library spectra objects
				ArrayList<RankedLibrarySpectrum> resultList = new ArrayList<RankedLibrarySpectrum>();
				
				// prepare query spectrum for similarity comparison with candidate spectra,
				// e.g. vectorize peaks, calculate auto-correlation, etc.
				procSet.getSpecComparator().prepare(mgfQuery.getHighestPeaks(procSet.getPickCount()));
				
				// iterate candidates
				for (SpectralSearchCandidate candidate : candidates) {
					// re-check precursor tolerance criterion to determine proper candidates
					if (Math.abs(mgfQuery.getPrecursorMZ() - candidate.getPrecursorMz()) < procSet.getTolMz()) {
						// TODO: redundancy check in candidates (e.g. same spectrum from multiple peptide associations)
						// score query and library spectra
						procSet.getSpecComparator().compareTo(candidate.getPeaks());
						double score = procSet.getSpecComparator().getSimilarity();
						
						// store result if score is above specified threshold
						if (score >= procSet.getThreshScore()) {
							// TODO: finish storage in RankedLibrarySpectrum objects, map everything to peptides and proteins
							
							// store peptide ID in map for annotation gathering later on
							
							// create MascotGenericFile from SpectralSearchCandidate object
							MascotGenericFile mgfLib = new MascotGenericFile(null, candidate.getSpectrumTitle(), candidate.getPeaks(), candidate.getPrecursorMz(), candidate.getPrecursorCharge());
							
							resultList.add(new RankedLibrarySpectrum(mgfLib, candidate.getSpectrumID(), candidate.getSequence(), null, score));
						}
					}
				}
				procSet.getSpecComparator().cleanup();
				resultMap.put(mgfQuery.getTitle(), resultList);
				pSupport.firePropertyChange("progressmade", 0, 1);
//				pSupport.firePropertyChange("progress", progress++, progress);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultMap;
	}
		
	/**
	 * Method to consolidate spectra which are selected in a specified checkbox tree into spectrum packages of defined size.
	 * @param packageSize The amount of spectra per package.
	 * @param checkBoxTree The checkbox tree.
	 * @param listener An optional property change listener used to monitor progress.
	 * @return A list of files.
	 * @throws IOException 
	 */
	public List<File> packSpectra(int packageSize, CheckBoxTreeManager checkBoxTree, String filename) throws IOException {
		List<File> files = new ArrayList<File>();
		FileOutputStream fos = null;
		CheckBoxTreeSelectionModel selectionModel = checkBoxTree.getSelectionModel();
		DefaultMutableTreeNode fileRoot = (DefaultMutableTreeNode) checkBoxTree.getModel().getRoot();
		int numSpectra = 0;
		DefaultMutableTreeNode spectrumNode = fileRoot.getFirstLeaf();
		if (spectrumNode != fileRoot) {
			// iterate over all leaves
			while (spectrumNode != null) {
				// generate tree path and consult selection model whether path is explicitly or implicitly selected
				TreePath spectrumPath = new TreePath(spectrumNode.getPath());
				if (selectionModel.isPathSelected(spectrumPath, true)) {
					if ((numSpectra % packageSize) == 0) {			// create a new package every x files
						if (fos != null) {
							fos.close();
						}
						File file = new File(filename + (numSpectra/packageSize) + ".mgf");
						files.add(file);
						fos = new FileOutputStream(file);
					}
					MascotGenericFile mgf = ((SpectrumTree)checkBoxTree.getTree()).getSpectrumAt(spectrumNode);
					mgf.writeToStream(fos);
					fos.flush();
					try {
						pSupport.firePropertyChange("progress", numSpectra++, numSpectra);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				spectrumNode = spectrumNode.getNextLeaf();
			}
			fos.close();
		} else {
			throw new IOException("ERROR: No files selected.");
		}
		return files;
	}

	// XXX: TBD
	public List<MascotGenericFile> downloadSpectra(long experimentID) throws Exception {
		return new SpectrumExtractor(conn).downloadSpectra(experimentID);
	} 
	
	// Thread polling the server each second.
	class RequestThread extends Thread {		
		public void run() {
			while(true){
				try {
					Thread.sleep(1000);
					request();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
     * Adds a property change listener.
     * @param pcl
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) { 
    	pSupport.addPropertyChangeListener(pcl); 
    }
	
	/**
     * Removes a property change listener.
     * @param pcl
     */
    public void removePropertyChangeListener(PropertyChangeListener pcl) { 
    	pSupport.removePropertyChangeListener(pcl); 
    }
    
    /**
     * Returns the current connection to the database.
     * @return
     * @throws SQLException 
     */
    public Connection getConnection() throws SQLException{
    	if(conn == null) initDBConnection();
    	return conn;
    }
}
