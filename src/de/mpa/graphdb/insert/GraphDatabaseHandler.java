package de.mpa.graphdb.insert;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.Keyword;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.dbx.ko.KO;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

import de.mpa.analysis.UniprotAccessor;
import de.mpa.analysis.UniprotAccessor.KeywordOntology;
import de.mpa.client.Client;
import de.mpa.client.model.SpectrumMatch;
import de.mpa.client.model.dbsearch.DbSearchResult;
import de.mpa.client.model.dbsearch.PeptideHit;
import de.mpa.client.model.dbsearch.PeptideSpectrumMatch;
import de.mpa.client.model.dbsearch.ProteinHit;
import de.mpa.graphdb.cypher.CypherQuery;
import de.mpa.graphdb.edges.RelationType;
import de.mpa.graphdb.io.GraphMLHandler;
import de.mpa.graphdb.nodes.NodeType;
import de.mpa.graphdb.properties.EdgeProperty;
import de.mpa.graphdb.properties.EnzymeProperty;
import de.mpa.graphdb.properties.OntologyProperty;
import de.mpa.graphdb.properties.PathwayProperty;
import de.mpa.graphdb.properties.PeptideProperty;
import de.mpa.graphdb.properties.ProteinProperty;
import de.mpa.graphdb.properties.PsmProperty;
import de.mpa.graphdb.properties.TaxonProperty;
import de.mpa.graphdb.setup.GraphDatabase;

/**
 * GraphDatabaseHandler provides possibilities to insert data into the graph database.
 * 
 * @author Thilo Muth
 * @date 2013-04-17
 * @version 0.7.0
 *
 */
public class GraphDatabaseHandler {
	
	/**
	 *  Data to be inserted in the graph.
	 */
	private Object data;
	
	/**
	 * By default, the first operation on a TransactionalGraph will start a transaction automatically.
	 */
	private TransactionalGraph graph;
	
	/**
	 *  An IndexableGraph is a graph that supports the manual indexing of its elements.
	 */
	private IndexableGraph indexGraph;
	
	/**
	 * GraphDatabase object.
	 */
	private GraphDatabase graphDb;
	
	private Index<Vertex> proteinIndex;
	private Index<Vertex> peptideIndex;
	private Index<Vertex> psmIndex;
	private Index<Vertex> speciesIndex;
	private Index<Vertex> enzymeIndex;
	private Index<Vertex> pathwayIndex;
	private Index<Vertex> ontologyIndex;
	private Index<Edge> edgeIndex;

	private ExecutionEngine engine;
	
	
	public GraphDatabaseHandler(GraphDatabase graphDb) {
		this.graphDb = graphDb;
		indexGraph = new Neo4jGraph(graphDb.getService());
		graph = new Neo4jGraph(graphDb.getService());
		setupIndices();
		
	}
	
	/**
	 * Exports the graph to an GraphML output file.
	 */
	public void exportGraph(File outputFile) {
		GraphMLHandler.exportGraphML(graph, outputFile);
	}
	
	/**
	 * Sets the data object.
	 */
	public void setData(Object data) {
		this.data = data;
		insert();
		setupExecutionEngine();
	}
	
	/**
	 * Initializes the execution engine instance.
	 */
	private void setupExecutionEngine() {
		engine = new ExecutionEngine(graphDb.getService());
		
	}

	/**
	 * This method inserts the data into the graph database and stops the transaction afterwards.
	 */
	private void insert() {
		if (data instanceof DbSearchResult) {
			DbSearchResult dbSearchResult = (DbSearchResult) data;
			List<ProteinHit> proteinHits = dbSearchResult.getProteinHitList();
			
			Client client = Client.getInstance();
			client.firePropertyChange("new message", null, "BUILDING GRAPH DATABASE");
			client.firePropertyChange("resetall", -1L, Long.valueOf(proteinHits.size()));
			
			for (ProteinHit proteinHit : proteinHits) {
				addProtein(proteinHit);
				client.firePropertyChange("progressmade", -1L, 0L);
			}
			
			client.firePropertyChange("new message", null, "BUILDING GRAPH DATABASE FINISHED");
		}
		
		// Stop the transaction
		stopTransaction();
	}
	
	/**
	 * Adds a protein to the graph as vertex.
	 * @param protHit Protein hit.
	 */
	private void addProtein(ProteinHit protHit) {
		Vertex proteinVertex =  graph.addVertex(null);
		
		String accession = protHit.getAccession();
		
		proteinVertex.setProperty(ProteinProperty.IDENTIFIER.toString(), accession);
		proteinVertex.setProperty(ProteinProperty.DESCRIPTION.toString(), protHit.getDescription());
		proteinVertex.setProperty(ProteinProperty.SEQUENCE.toString(), protHit.getSequence());
		proteinVertex.setProperty(ProteinProperty.LENGTH.toString(), protHit.getSequence().length());
		proteinVertex.setProperty(ProteinProperty.COVERAGE.toString(), protHit.getCoverage());
		
		// Index the proteins by their accession.
		proteinIndex.put(ProteinProperty.IDENTIFIER.toString(), accession, proteinVertex);
		
		// Add peptides.
		addPeptides(protHit.getPeptideHitList(), proteinVertex);
		
		// Add species.
		addSpecies(protHit.getSpecies(), proteinVertex);
		
		// Add enzyme numbers.
		UniProtEntry uniprotEntry = protHit.getUniprotEntry();
		// TODO: Null check valid here ?
		if (uniprotEntry != null) {
			
			List<String> ecNumbers = uniprotEntry.getProteinDescription().getEcNumbers();
			addEnzymeNumbers(ecNumbers, proteinVertex);
			
			// Add pathways. 
			List<DatabaseCrossReference> xRefs = uniprotEntry.getDatabaseCrossReferences(DatabaseType.KO);
			addPathways(xRefs, proteinVertex);
			
			// Add ontologies.
			addOntologies(protHit, proteinVertex);
		}
	}
	
	/**
	 * Adds a species to the graph as vertex.
	 * @param species Species to be added.
	 * @param proteinVertex Involved protein vertex (outgoing edge).
	 */
	private void addSpecies(String species, Vertex proteinVertex) {
		if (species == null) {
			species = "Unknown";
		}
		Vertex speciesVertex = null;
		Iterator<Vertex> speciesIterator = speciesIndex.get(TaxonProperty.IDENTIFIER.toString(), species).iterator();
			if (speciesIterator.hasNext()) {
				speciesVertex = speciesIterator.next();
			} else {
				// Create new vertex.
				speciesVertex = graph.addVertex(null);
				speciesVertex.setProperty(TaxonProperty.IDENTIFIER.toString(), species);
				// Index the species by the species name.
				speciesIndex.put(TaxonProperty.IDENTIFIER.toString(), species, speciesVertex);
			}
			// Add edge between protein and species.
			addEdge(proteinVertex, speciesVertex, RelationType.BELONGS_TO);
	}
	
	/**
	 * Adds an enzyme number to the graph as vertex.
	 * @param ecNumber Enzyme number to be added.
	 * @param proteinVertex Involved protein vertex (outgoing edge).
	 */
	private void addEnzymeNumbers(List<String> ecNumbers, Vertex proteinVertex) {
		Vertex enzymeVertex = null;
		for (String ecNumber : ecNumbers) {
			Iterator<Vertex> vertexIterator =
					enzymeIndex.get(EnzymeProperty.IDENTIFIER.toString(), ecNumber).iterator();
			if (vertexIterator.hasNext()) {
				enzymeVertex = vertexIterator.next();
			} else {
				// Split E.C. number string of the format '1.2.3.4'
				String[] ecTokens = ecNumber.split("\\.");
				for (int i = 0; i < ecTokens.length; i++) {
					// Check tokens, abort prematurely if '-' is found
					String ecToken = ecTokens[i];
					if ("-".equals(ecToken)) {
						break;
					}
					// Build partial E.C. number, e.g. '1.2.-.-'
					String ecFragment = ecTokens[0];
					for (int j = 1; j < ecTokens.length; j++) {
						ecFragment += "." + ((j <= i) ? ecTokens[j] : "-");
					}
					// Get existing vertex for (partial) E.C. number...
					Vertex temp = graph.getVertex(ecFragment);
					if (temp == null) {
						// ... or create new vertex.
						temp = graph.addVertex(ecFragment);
						temp.setProperty(EnzymeProperty.IDENTIFIER.toString(), ecFragment);
						// TODO set description property, fetch from intenz.xml (ftp://ftp.ebi.ac.uk/pub/databases/intenz/xml/)
					}
					// Index vertex by E.C. Number.
					enzymeIndex.put(EnzymeProperty.IDENTIFIER.toString(), ecFragment, temp);
					
					// Link enzyme vertices
					if (i > 0) {
						addEdge(enzymeVertex, temp, RelationType.IS_SUPERGROUP_OF);
					}
					
					enzymeVertex = temp;
				}
				
			}
			// Add edge between protein and enzyme number.
			addEdge(proteinVertex, enzymeVertex, RelationType.BELONGS_TO_ENZYME);
		}
	}
	
	/**
	 * Adds KEGG pathways to the database.
	 * @param xRefs List of database cross references.
	 * @param proteinVertex Involved protein vertex (outgoing edge).
	 */
	private void addPathways(List<DatabaseCrossReference> xRefs, Vertex proteinVertex) {
		Vertex pathwayVertex = null;
		for (DatabaseCrossReference xRef : xRefs) {
			String koNumber = ((KO) xRef).getKOIdentifier().getValue();
			Iterator<Vertex> pathwayIterator =
					pathwayIndex.get(PathwayProperty.IDENTIFIER.toString(), koNumber).iterator();
			if (pathwayIterator.hasNext()) {
				pathwayVertex = pathwayIterator.next();
			} else {
				// Create new vertex.
				pathwayVertex = graph.addVertex(null);
				pathwayVertex.setProperty(PathwayProperty.IDENTIFIER.toString(), koNumber);
				//TODO: Add KEGG description + KEGG Number!

				// Index the pathway by the ID.
				pathwayIndex.put(PathwayProperty.IDENTIFIER.toString(), koNumber, pathwayVertex);
			}
			// Add edge between protein and pathway.
			addEdge(proteinVertex, pathwayVertex, RelationType.BELONGS_TO_PATHWAY);
		}
	}
	
	/**
	 * Adds ontologies to the graph database.
	 * @param protHit
	 * @param proteinVertex
	 */
	private void addOntologies(ProteinHit protHit, Vertex proteinVertex) {
		Map<String, KeywordOntology> ontologyMap = UniprotAccessor.ONTOLOGY_MAP;
		UniProtEntry entry = protHit.getUniprotEntry();
		Vertex ontologyVertex = null;
		
		// Entry must be provided
		if (entry != null) {
			List<Keyword> keywords = entry.getKeywords();
			for (Keyword kw : keywords) {
				String keyword = kw.getValue();
				if (ontologyMap.containsKey(keyword)) {
					KeywordOntology type = ontologyMap.get(keyword);
					
					// Check if peptide is already contained in the graph.
					Iterator<Vertex> ontologyIterator =
							ontologyIndex.get(OntologyProperty.KEYWORD.toString(), keyword).iterator();
					if (ontologyIterator.hasNext()) {
						ontologyVertex = ontologyIterator.next();
					} else {
						// Create new vertex.
						ontologyVertex = graph.addVertex(null);
						ontologyVertex.setProperty(OntologyProperty.KEYWORD.toString(), keyword);
						ontologyVertex.setProperty(OntologyProperty.TYPE.toString(), type.toString());
						
						// Index the proteins by their accession.
						ontologyIndex.put(OntologyProperty.KEYWORD.toString(), keyword, ontologyVertex);
					}			
					
					// Add edge between protein and ontology.
					switch (type) {
					case BIOLOGICAL_PROCESS:
						addEdge(proteinVertex, ontologyVertex, RelationType.INVOLVED_IN_BIOPROCESS);
						break;
					case CELLULAR_COMPONENT:
						addEdge(proteinVertex, ontologyVertex, RelationType.BELONGS_TO_COMPONENT);
						break;
					case MOLECULAR_FUNCTION:
						addEdge(proteinVertex, ontologyVertex, RelationType.HAS_MOLECULAR_FUNCTION);
						break;
					}
					
				}
			}
		}
	}
	
	/**
	 * Adds peptides to the graph.
	 * @param peptideHitList The peptide list
	 */
	private void addPeptides(List<PeptideHit> peptideHitList, Vertex proteinVertex) {
		for (PeptideHit peptideHit : peptideHitList) {
			String sequence = peptideHit.getSequence();
			Vertex peptideVertex = null;
			// Check if peptide is already contained in the graph.
			Iterator<Vertex> peptideIterator =
					peptideIndex.get(PeptideProperty.IDENTIFIER.toString(), sequence).iterator();
			if (peptideIterator.hasNext()) {
				peptideVertex = peptideIterator.next();
			} else {
				// Create new vertex.
				peptideVertex = graph.addVertex(null);
				peptideVertex.setProperty(PeptideProperty.IDENTIFIER.toString(), peptideVertex.getId().toString());
				peptideVertex.setProperty(PeptideProperty.SEQUENCE.toString(), sequence);
				peptideVertex.setProperty(PeptideProperty.LENGTH.toString(), sequence.length());
				
				// Index the proteins by their accession.
				peptideIndex.put(PeptideProperty.IDENTIFIER.toString(), sequence, peptideVertex);
			}			
			
			// Add edge between peptide and protein.
			addEdge(proteinVertex, peptideVertex, RelationType.HAS_PEPTIDE);
		
			// Add PSMs.
			addPeptideSpectrumMatches(peptideHit.getSpectrumMatches(), peptideVertex);
		}
	}
	
	/**
	 * Adds PSMs to the graph.
	 * @param peptideHitList The peptide list
	 */
	private void addPeptideSpectrumMatches(List<SpectrumMatch> spectrumMatches, Vertex peptideVertex) {
		for (SpectrumMatch sm : spectrumMatches) {
			Vertex psmVertex =  null;
			PeptideSpectrumMatch psm = (PeptideSpectrumMatch) sm;
			long spectrumID = psm.getSearchSpectrumID();
			
			// Check if PSM is already contained in the graph.
			Iterator<Vertex> psmIterator =
					psmIndex.get(PsmProperty.SPECTRUMID.toString(), spectrumID).iterator();
			if (psmIterator.hasNext()) {
				psmVertex = psmIterator.next();
			} else {
				// Create new vertex.
				psmVertex = graph.addVertex(null);
				//TODO: Use spectrum title as property too!
				psmVertex.setProperty(PsmProperty.SPECTRUMID.toString(), spectrumID);
				// TODO: psmVertex.setProperty(PsmProperty.VOTES.toString(), psm.getVotes());
				
				// Index the proteins by their accession.
				psmIndex.put(PsmProperty.SPECTRUMID.toString(), spectrumID, psmVertex);
			}	
			addEdge(psmVertex, peptideVertex, RelationType.IS_MATCH_IN);
		}
	}
	
	/**
	 * Adds an edge to the graph and creates a unique ID from both involved vertices.
	 * Also checks whether edge is already existing in the graph.
	 * @param outVertex The outgoing vertex.
	 * @param inVertex The incoming vertex.
	 * @param label The edge label.
	 */
	private void addEdge(final Vertex outVertex, final Vertex inVertex, final RelationType relType) {
		
		String id = outVertex.getId()+ "_" + inVertex.getId();
		// Check if edge is not already contained in the graph.
		if (!edgeIndex.get(EdgeProperty.ID.toString(), id).iterator().hasNext()) {
			Edge edge = graph.addEdge(id, outVertex, inVertex, relType.name());
			edge.setProperty(EdgeProperty.LABEL.name(), relType.name());
			edgeIndex.put(EdgeProperty.ID.toString(), id, edge);
		} 
	}
	
	/**
	 * Method sets up the indices graph.
	 */
	public void setupIndices() {
		// Protein index
		proteinIndex = indexGraph.getIndex(NodeType.PROTEINS.toString(), Vertex.class);
		
		// If not existent, create a new index.
		if (proteinIndex == null) {
			proteinIndex = indexGraph.createIndex(NodeType.PROTEINS.toString(), Vertex.class);
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
		
		// Peptide Index
		peptideIndex = indexGraph.getIndex(NodeType.PEPTIDES.toString(), Vertex.class);
		// If not existent, create a new index.
		if (peptideIndex == null) {
			peptideIndex = indexGraph.createIndex(NodeType.PEPTIDES.toString(), Vertex.class);
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
		
		// PSM index
		psmIndex = indexGraph.getIndex(NodeType.PSMS.toString(), Vertex.class);
		// If not existent, create a new index.
		if (psmIndex == null) {
			psmIndex = indexGraph.createIndex(NodeType.PSMS.toString(), Vertex.class);
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
		
		// Species index
		speciesIndex = indexGraph.getIndex(NodeType.TAXA.toString(), Vertex.class);
		// If not existent, create a new index.
		if (speciesIndex == null) {
			speciesIndex = indexGraph.createIndex(NodeType.TAXA.toString(), Vertex.class);			
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
		
		// Enzyme index
		enzymeIndex = indexGraph.getIndex(NodeType.ENZYMES.toString(), Vertex.class);
		// If not existent, create a new index.
		if (enzymeIndex == null) {
			enzymeIndex = indexGraph.createIndex(NodeType.ENZYMES.toString(), Vertex.class);
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
		
		// Pathway index
		pathwayIndex = indexGraph.getIndex(NodeType.PATHWAYS.toString(), Vertex.class);
		// If not existent, create a new index.
		if (pathwayIndex == null) {
			pathwayIndex = indexGraph.createIndex(NodeType.PATHWAYS.toString(), Vertex.class);
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
		
		// Ontology index
		ontologyIndex = indexGraph.getIndex(NodeType.ONTOLOGIES.toString(), Vertex.class);
		// If not existent, create a new index.
		if (ontologyIndex == null) {
			ontologyIndex = indexGraph.createIndex(NodeType.ONTOLOGIES.toString(), Vertex.class);
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
		
		// Edge index
		edgeIndex = indexGraph.getIndex("edges", Edge.class);
		// If not existent, create a new index.
		if (edgeIndex == null) {
			edgeIndex = indexGraph.createIndex("edges", Edge.class);
			((TransactionalGraph) indexGraph).stopTransaction(Conclusion.SUCCESS);
		}
	}

	/**
	 * Returns the graph database service.
	 * @return {@link GraphDatabaseService}
	 */
	public GraphDatabaseService getGraphDatabaseService() {
		return graphDb.getService();
	}
	
	/**
	 * Executes a cypher query {@link CypherQuery}.
	 * @param cypherQuery {@link CypherQuery}
	 * @return {@link ExecutionResult}
	 * @throws Exception 
	 */
	public ExecutionResult executeCypherQuery(CypherQuery cypherQuery) throws Exception {
		// Validate query
		if (cypherQuery.isValid()) {
			return engine.execute(cypherQuery.toString());
		} else {
			throw new Exception("Invalid Cypher Query");
		}
	}

	/**
	 * Stops the transaction of a graph database.
	 */
	public void stopTransaction() {
		graph.stopTransaction(Conclusion.SUCCESS);
		((TransactionalGraph)indexGraph).stopTransaction(Conclusion.SUCCESS);
		
	}
	
	/**
	 * Shuts down the graph database.
	 */
	public void shutDown() {
		graphDb.shutDown();
	}
	
}
