package de.mpa.graphdb.cypher;

import static org.neo4j.helpers.collection.MapUtil.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.mpa.graphdb.nodes.NodeType;
import de.mpa.graphdb.properties.ElementProperty;
import de.mpa.graphdb.properties.ProteinProperty;

/**
 * Query class using the Cypher query language.
 * @author Thilo Muth
 *
 */
public class CypherQuery {
	
	/**
	 * The list of starting nodes of the query.
	 */
	private List<CypherStartNode> startNodes;

	/**
	 * The list of match statements of the query.
	 */
	private List<CypherMatch> matches;
	
	/**
	 * The list of conditionals of the query.
	 */
	private List<CypherCondition> conditions;
	
	/**
	 * The list of return values of the query as indices of the matches used.
	 */
	private List<Integer> returnIndices;
	
	/**
	 * Query statement.
	 */
	private String statement;
	
	/**
	 * Query state of being custom-built or not.
	 */
	private boolean custom;
	
	/**
	 * Title of the CypherQuery.
	 */
	private String title;
	
	/**
	 * CypherQuery constructor for a query made from direct statement, i.e. query is custom-built.
	 * @param statement
	 */
	public CypherQuery(String statement) {
		this.statement = statement;
		this.custom = true;
	}
	
	/**
	 * CypherQuery constructor for query made from defined start nodes, matches, conditions and return indices. 
	 * @param startNodes Cypher start nodes
	 * @param matches Cypher matches
	 * @param conditions Cypher matches
	 * @param returnIndices Cypher return indices
	 */
	public CypherQuery(List<CypherStartNode> startNodes, List<CypherMatch> matches, List<CypherCondition> conditions, List<Integer> returnIndices) {
		this.startNodes = startNodes;
		this.matches = matches;
		this.conditions = conditions;
		this.returnIndices = returnIndices;
	}

	/**
	 * Returns whether the query is valid in its current state.
	 * @return <code>true</code> if the query is valid, <code>false</code> otherwise
	 */
	public boolean isValid() {
		// Assume that the provided statement is valid. 
		if(statement != null) return true;		
		return (startNodes != null) && (returnIndices != null);
	}
	
	@Override
	public String toString() {
		
		// Returns direct statement
		if(statement != null) return statement;
		
		// Begin with START line
		String statement = "START ";
		boolean first = true;
		for (CypherStartNode startNode : startNodes) {
			if (!first) {
				statement += ", ";
			}
			statement += startNode;
			first = false;
		}
		// Add matches, if any were defined
		if ((matches != null) && (matches.size() > 1)) {
			statement += "\nMATCH ";
			for (CypherMatch match : matches) {
				statement += match;
			}
		}
		if (conditions != null) {
			statement += "\nWHERE ";
			for (CypherCondition cond : conditions) {
				statement += cond;
			}
		}
		statement += "\nRETURN ";
		first = true;
		for (Integer returnIndex : returnIndices) {
			if (!first) {
				statement += ", ";
			}
			statement += matches.get(returnIndex).getNodeIdentifier();
			first = false;
		}
		
		return statement;
	}

	/**
	 * Returns the list of start nodes.
	 * @return the start nodes
	 */
	public List<CypherStartNode> getStartNodes() {
		return startNodes;
	}
	
	/**
	 * Sets the list of start nodes.
	 * @param startNodes the list of start nodes to set
	 */
	public void setStartNodes(List<CypherStartNode> startNodes) {
		this.startNodes = startNodes;
	}

	/**
	 * Returns the list of matches.
	 * @return the matches
	 */
	public List<CypherMatch> getMatches() {
		return matches;
	}

	/**
	 * Sets the list of matches.
	 * @param the matches to set
	 */
	public void setMatches(List<CypherMatch> matches) {
		this.matches = matches;
	}
	
	/**
	 * Returns the list of return indices.
	 * @return the return indices
	 */
	public List<Integer> getReturnIndices() {
		return returnIndices;
	}

	/**
	 * Sets the list of return indices.
	 * @param returnIndices the return indices to set
	 */
	public void setReturnIndices(List<Integer> returnIndices) {
		this.returnIndices = returnIndices;
	}
	
	/**
	 * Returns the state of the CypherQuery (being custom-built or not).
	 * @return true if the CypherQuery is custom-built else false
	 */
	public boolean isCustom() {
		return custom;
	}
	
	/**
	 * Returns the title of the query.
	 * @return
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Sets the title of the query.
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/* only legacy code below this line - TODO: remove/refactor */

	/**
	 * Execution engine.
	 */
    private ExecutionEngine engine = null;
    
    /**
     * Construct the CypherQuery, providing method to do user-specific queries against the graph database.
     * @param graphDb GraphDatabaseService object
     */
    public CypherQuery(GraphDatabaseService graphDb) {
    	// TODO: destroy this constructor!!
        engine = new ExecutionEngine(graphDb);
    }
    
    /**
     * Executes a general Cypher query and returns ExecutionResult object.
     * @param query Query string (in cypher query language)
     * @return ExecutionResult
     */
    public ExecutionResult execute(String query) {
    	return engine.execute(query);
    }
    
    /**
     * Returns the first inserted node of the graph.
     * @return First node ExecutionResult 
     */
    public ExecutionResult getFirstNode() {
        return engine.execute( "start n=node(1) return n" );
	}
	
    /**
     * Returns the protein by its accession.
     * @param accession Protein accession
     * @return Protein node ExecutionResult
     */
	public ExecutionResult getProteinByAccession(String accession) {
        return engine.execute("START protein=node:proteins(IDENTIFIER = {accession}) return protein", map("accession", accession));
	}
	
    /**
     * Returns the protein by its sequence.
     * @param accession Protein sequence
     * @return Protein node ExecutionResult
     */
	public ExecutionResult getProteinBySequence(String sequence) {
		return engine.execute("START protein=node:proteins(PROTEINSEQUENCE = {sequence}) return protein", map("sequence", sequence));
	}
	
	/**
	 * Returns all peptides for a protein (specified by its accession).
	 * @param accession Protein accession
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getPeptidesForProtein(String accession) {
		return engine.execute("START protein=node:proteins(IDENTIFIER = {accession}) " +
                "MATCH (protein)-[:HAS_PEPTIDE]->(peptide) " +
                "RETURN peptide", map("accession", accession));
	}
	
	/**
	 * Returns all shared peptides of the dataset.
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getAllSharedPeptides() {
		return engine.execute("START aPeptide=node:" + NodeType.PEPTIDES + "(\"SEQUENCE:*\") " +
				"MATCH (aPeptide)<-[rel:HAS_PEPTIDE]-(bProtein) " + 
				"WITH aPeptide, count(rel) as cn " +
				"WHERE cn > 1 " + 
				"RETURN aPeptide");
	}
	
	/**
	 * Returns all proteins of the dataset.
	 * @return
	 */
	public ExecutionResult getAllProteins() {
		return engine.execute("START bProtein=node:proteins(\"IDENTIFIER:*\") " +
				"MATCH (bProtein)-[:HAS_PEPTIDE]->(aPeptide) " +
				"RETURN bProtein, aPeptide");
	}
	
	public ExecutionResult getAllEnzymes() {
		return engine.execute("START eProtein=node:proteins(\"IDENTIFIER:*\") " +
				"MATCH (eProtein)-[:BELONGS_TO_ENZYME]->(dE4)<-[:IS_SUPERGROUP_OF]-(cE3)<-[:IS_SUPERGROUP_OF]-(bE2)<-[:IS_SUPERGROUP_OF]-(aE1) " +
				"RETURN aE1, bE2, cE3, dE4, eProtein");
	}
	
	/**
	 * Returns the unique peptides for a protein (specified by its accession).
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getUniquePeptidesForProtein(String accession) {
		return engine.execute("START protein=node:" + NodeType.PROTEINS + "(" + ProteinProperty.IDENTIFIER + " = {accession}) " +
				"MATCH (protein)-[:HAS_PEPTIDE]->(peptide) " +
				"WITH peptide " +
				"MATCH (peptide)<-[rel:HAS_PEPTIDE]-(protein2) " +
				"WITH peptide, count(rel) as cn " + 
				"WHERE cn = 1 " + 
				"RETURN peptide", map("accession", accession));
	}
	
	/**
	 * Returns the shared peptides for a protein (specified by its accession).
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getSharedPeptidesForProtein(String accession) {
		return engine.execute("START protein=node:proteins(IDENTIFIER = {accession}) " +
				"MATCH (protein)-[:HAS_PEPTIDE]->(peptide) " +
				"WITH peptide " +
				"MATCH (peptide)<-[rel:HAS_PEPTIDE]-(protein2) " +
				"WITH peptide, count(rel) as cn " + 
				"WHERE cn > 1 " + 
				"RETURN peptide", map("accession", accession));
	}	

	
	/**
	 * Returns all peptides for an enzyme (specified by its E.C. number). 
	 * @param ecNumber Enzyme E.C. number
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getPeptidesForEnzyme(String ecNumber) {
		return engine.execute("START enzyme=node:enzymes(ECNUMBER = {ecnumber}) " +
                "MATCH (enzyme)<-[:BELONGS_TO_ENZYME]-(protein)-[:HAS_PEPTIDE]->(peptide) " +
                "RETURN peptide", map("ecnumber", ecNumber));
	}
	
	/**
	 * Returns all peptides for a pathway (specified by its KO number). 
	 * @param koNumber Pathway KO number
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getPeptidesForPathway(String koNumber) {
		return engine.execute("START pathway=node:pathways(KONUMBER = {konumber}) " +
                "MATCH (pathway)<-[:BELONGS_TO_PATHWAY]-(protein)-[:HAS_PEPTIDE]->(peptide) " +
                "RETURN peptide", map("konumber", koNumber));
	}
	
	/**
	 * Returns all peptides for biological process (specified by keyword). 
	 * @param keyword Ontology keyword
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getPeptidesForBiologicalProcess(String keyword) {
		return engine.execute("START ontology=node:ontologies(KEYWORD = {keyword}) " +
                "MATCH (ontology)<-[:INVOLVED_IN_BIOPROCESS]-(protein)-[:HAS_PEPTIDE]->(peptide) " +
                "RETURN peptide", map("keyword", keyword));
	}
	
	/**
	 * Returns all peptides for molecular function (specified by keyword). 
	 * @param keyword Ontology keyword
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getPeptidesForMolecularFunction(String keyword) {
		return engine.execute("START ontology=node:ontologies(KEYWORD = {keyword}) " +
                "MATCH (ontology)<-[:HAS_MOLECULAR_FUNCTION]-(protein)-[:HAS_PEPTIDE]->(peptide) " +
                "RETURN peptide", map("keyword", keyword));
	}
	
	/**
	 * Returns all peptides for cellular component (specified by keyword). 
	 * @param keyword Ontology keyword
	 * @return Peptide node(s) ExecutionResult
	 */
	public ExecutionResult getPeptidesForCellularComponent(String keyword) {
		return engine.execute("START ontology=node:ontologies(KEYWORD = {keyword}) " +
                "MATCH (ontology)<-[:BELONGS_TO_COMPONENT]-(protein)-[:HAS_PEPTIDE]->(peptide) " +
                "RETURN peptide", map("keyword", keyword));
	}
	
	/**
	 * Returns all PSMS for a peptide (specified by its sequence):
	 * peptide<-IS_MATCH_IN-psm
	 * @param sequence Peptide sequence
	 * @return PSM node(s) ExecutionResult
	 */
	public ExecutionResult getPSMsForPeptide(String sequence) {
		return engine.execute("START peptide=node:peptides(SEQUENCE = {sequence}) " +
                "MATCH (peptide)<-[:IS_MATCH_IN]-(psm) " +
                "RETURN psm", map("sequence", sequence));
	}
	
	/**
	 * Returns all PSMS for a protein (specified by its accession):
	 * Protein-HAS_PEPTIDE->Peptide<-IS_MATCH_IN-Psm
	 * @param accession Protein accession
	 * @return PSM node(s) ExecutionResult
	 */
	public ExecutionResult getPSMsForProtein(String accession) {
		return engine.execute("START protein=node:proteins(IDENTIFIER = {accession}) " +
                "MATCH (protein)-[:HAS_PEPTIDE]->(peptide)<-[:IS_MATCH_IN]-(psm) " +
                "RETURN psm", map("accession", accession));
	}
	
	/**
	 * Returns all PSMS for an enzyme (specified by its E.C. number):
	 * Enzyme<--Protein-->Peptide<--PSM
	 * @param ecNumber Enzyme number
	 * @return PSM node(s) ExecutionResult
	 */
	public ExecutionResult getPSMsForEnzyme(String ecNumber) {
		return engine.execute("START enzyme=node:enzymes(IDENTIFIER = {ecNumber}) " +
                "MATCH (enzyme)<-[:BELONGS_TO_ENZYME]-(protein)-[:HAS_PEPTIDE]->(peptide)<-[:IS_MATCH_IN]-(psm) " +
                "RETURN psm", map("ecNumber", ecNumber));
	}
	
	/**
	 * Returns all PSMS for a pathway (specified by its KO number):
	 * Pathway<--Protein-->Peptide<--PSM
	 * @param koNumber KEGG Orthology number
	 * @return PSM node(s) ExecutionResult
	 */
	public ExecutionResult getPSMsForPathway(String koNumber) {
		return engine.execute("START pathway=node:pathways(KONUMBER = {koNumber}) " +
                "MATCH (pathway)<-[:BELONGS_TO_PATHWAY]-(protein)-[:HAS_PEPTIDE]->(peptide)<-[:IS_MATCH_IN]-(psm) " +
                "RETURN psm", map("koNumber", koNumber));
	}
	
	/**
	 * Returns all PSMs for biological process (specified by keyword). 
	 * @param keyword Ontology keyword
	 * @return PSM node(s) ExecutionResult
	 */
	public ExecutionResult getPSMsForBiologicalProcess(String keyword) {
		return engine.execute("START ontology=node:ontologies(KEYWORD = {keyword}) " +
                "MATCH (ontology)<-[:INVOLVED_IN_BIOPROCESS]-(protein)-[:HAS_PEPTIDE]->(peptide)<-[:IS_MATCH_IN]-(psm) " +
                "RETURN psm", map("keyword", keyword));
	}
	
	/**
	 * Returns all PSMs for molecular function (specified by keyword). 
	 * @param keyword Ontology keyword
	 * @return PSM node(s) ExecutionResult
	 */
	public ExecutionResult getPSMsForMolecularFunction(String keyword) {
		return engine.execute("START ontology=node:ontologies(KEYWORD = {keyword}) " +
                "MATCH (ontology)<-[:HAS_MOLECULAR_FUNCTION]-(protein)-[:HAS_PEPTIDE]->(peptide)<-[:IS_MATCH_IN]-(psm) " +
                "RETURN psm", map("keyword", keyword));
	}
	
	/**
	 * Returns all PSMs for cellular component (specified by keyword). 
	 * @param keyword Ontology keyword
	 * @return PSM node(s) ExecutionResult
	 */
	public ExecutionResult getPSMsForCellularComponent(String keyword) {
		return engine.execute("START ontology=node:ontologies(KEYWORD = {keyword}) " +
                "MATCH (ontology)<-[:BELONGS_TO_COMPONENT]-(protein)-[:HAS_PEPTIDE]->(peptide)<-[:IS_MATCH_IN]-(psm) " +
                "RETURN psm", map("keyword", keyword));
	}
	
    /**
     * Returns the relationships from the protein. 
     * @return ExecutionResult object
     */
    public ExecutionResult getRelationshipsFromProtein() {
        return engine.execute("START protein=node(1) " +
                              "MATCH (protein)-[rel]->() " +
                              "RETURN rel" );
	}
	
	/**
	 * Returns all nodes in the graph (default limit = 50);
	 * @return Nodes ExecutionResult
	 */
	public ExecutionResult getAllNodes() {
        return engine.execute( "start n=node(*) return n limit 50" );
	}
	
	/**
	 * Range limit method.
	 * @param from Start of range index
	 * @param to End of range index
	 * @return Resultant range collection.
	 */
    public static Collection<Long> limitRange(int from, int to) {
        final Collection<Long> result = new ArrayList<Long>(to - from + 1);
        for (int i=from;i<=to;i++) result.add((long)i);
        return result;
    }
    
    /**
	 * Retrieves a unique node set from the ExecutionResult object for a certain property.
	 * @param executionResult ExecutionResult object
	 * @param columnName Column name
	 * @param property ElementProperty object
	 * @return Unique node set
	 */
	public static Set<Node> retrieveNodeSet(ExecutionResult executionResult, String columnName, ElementProperty property) {
		Iterator<Object> columnAs = executionResult.columnAs(columnName);
		Set<Node> nodeSet = new HashSet<Node>();
		
        while (columnAs.hasNext()) {
            final Object value = columnAs.next();
            if (value instanceof Node) {
                Node n = (Node)value;
                if(property == null) nodeSet.add(n);
                else if(n.hasProperty(property.toString().toUpperCase())){
                	nodeSet.add(n);
                }
            }
        }
        return nodeSet;
	}
	
    /**
     * Convenience method for ExecutionResult output.
     * @param msg Message string
     * @param result ExecutionResult object
     * @param column Column name
     */
    public static void printResult(String msg, ExecutionResult result, String column) {
        System.out.println(msg);
        
        Iterator<Map<String, Object>> iterator = result.iterator();
        while(iterator.hasNext()) {
        	Map<String, Object> map = iterator.next();
        	Set<Entry<String, Object>> entrySet = map.entrySet();
        	for (Entry<String, Object> entry : entrySet) {
				System.out.println(entry.getKey());
				System.out.println(entry.getValue().toString());
			}
        	System.out.println();
        }
        
//        while (columnAs.hasNext()) {
//            final Object value = columnAs.next();
//            if (value instanceof Node) {
//                Node n = (Node)value;
//                System.out.println(n);
//                System.out.println(n.getId());
//                for (String key : n.getPropertyKeys()) {
//                    System.out.println("{ " + key + " : " + n.getProperty(key)	+ "; id: " + n.getId() + " } ");
//                }
//            } else {
//                System.out.println("{ " + column + " : " + value + " } ");
//            }
//        }
    }
    
    /**
     * Convenience method for relationship output.
     * @param msg Message string
     * @param result ExecutionResult object
     * @param column Column name
     */
    public static void printRelationship(String msg, ExecutionResult result, String column) {
        System.out.println(msg);
        Iterator<Object> columnAs = result.columnAs(column);
        while (columnAs.hasNext()) {
            Relationship n = (Relationship) columnAs.next();
            for (String key : n.getPropertyKeys()) {
                System.out.println("{ " + key + " : " + n.getProperty(key)	+ " } ");
            }
        }
    }
    
}