package de.mpa.taxonomy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mpa.analysis.UniprotAccessor;
import de.mpa.analysis.UniprotAccessor.TaxonomyRank;
import de.mpa.client.Client;
import de.mpa.client.model.SpectrumMatch;
import de.mpa.client.model.dbsearch.PeptideHit;
import de.mpa.client.model.dbsearch.ProteinHit;
import de.mpa.client.model.dbsearch.ProteinHitList;
import de.mpa.db.accessor.Taxonomy;

/**
 * This class serves as utility class for various methods handling taxonomic issues.
 * 
 * @author T. Muth
 *
 */
public class TaxonomyUtils {

	/**
	 * Private constructor as class contains only static helper methods.
	 */
	private TaxonomyUtils() {}
	
	/**
	 * This method creates a taxonomy node which contains all ancestor taxonomy nodes up to the root node.
	 * @param taxID Taxonomy ID
	 * @param taxonomyMap Taxonomy Map containing taxonomy DB accessor objects.
	 * @return TaxonomyNode Taxonomy node in the end state.
	 */
	public static TaxonomyNode createTaxonomyNode(long taxID, Map<Long, Taxonomy> taxonomyMap) {
		boolean reachedRoot = false;
		Taxonomy current = taxonomyMap.get(taxID);
		TaxonomyNode currentNode = new TaxonomyNode((int) current.getTaxonomyid(), current.getRank(), current.getDescription());
		TaxonomyNode leafNode = currentNode;
		while (!reachedRoot) {
			// Start
			current = taxonomyMap.get(taxID);
			long parentID = current.getParentid();
			Taxonomy ancestor = taxonomyMap.get(parentID);
			if (ancestor.getParentid() == 0L) {
				reachedRoot = true;
			}
			// Check if ancestor is given and its rank is in our favored set. 
			if (ancestor != null && UniprotAccessor.TAXONOMY_MAP.containsKey(ancestor.getRank())) {
				TaxonomyNode parentNode = new TaxonomyNode(	(int) ancestor.getTaxonomyid(), ancestor.getRank(), ancestor.getDescription());
				currentNode.setParentNode(parentNode);
				currentNode = parentNode;
			}  
			taxID = parentID;
		}
		return leafNode;
	}
	
	/**
	 * This method created an uncategorized taxonomy node for protein with UniProt entries. 
	 * @return Uncategorized taxonomy node.
	 */
	public static TaxonomyNode createUncatogorizedTaxonomyNode() {
		TaxonomyNode rootNode = new TaxonomyNode(1, "root", "root");
		TaxonomyNode uncategorizedNode = new TaxonomyNode(0, "superkingdom", "Uncategorized", rootNode);
		return uncategorizedNode;
	}
	
	/**
	 * Returns the common taxonomy node above the two specified taxonomy nodes.
	 * @param taxonNode1 the first taxonomy node
	 * @param taxonNode2 the second taxonomy node
	 * @return the common taxonomy node
	 * @throws Exception 
	 */
	public static TaxonomyNode getCommonTaxonomyNode(TaxonomyNode taxonNode1, TaxonomyNode taxonNode2) throws Exception {
		// Get root paths of both taxonomy nodes
		TaxonomyNode[] path1 = taxonNode1.getPath();
		TaxonomyNode[] path2 = taxonNode2.getPath();
		TaxonomyNode ancestor;
		
		// Find last common element starting from the root
		int len = Math.min(path1.length, path2.length);
		if (len > 1) {
			ancestor = path1[0];	// initialize ancestor as root
			for (int i = 1; i < len; i++) {
				if (!path1[i].equals(path2[i])) {
					break;
				} 
				ancestor = path1[i];
			}
		} else {
			ancestor = taxonNode1;
		}
		return ancestor;
	}
	
	/**
	 * Method to go through a peptide set and define for each peptide hit the common taxonomy of the subsequent proteins.
	 * @param peptideSet. The peptide set.
	 * @throws Exception
	 */
	public static void getCommonTaxId4EachPeptide(Set<PeptideHit> peptideSet) throws Exception {
		
		// Map with taxonomy entries
		Map<Integer, TaxonomyNode> nodeMap = new HashMap<Integer, TaxonomyNode>();
		nodeMap.put(1, NcbiTaxonomy.ROOT_NODE);
		
		// Go through whole peptideSet and check for all proteineEntries the common taxonomy
		for (PeptideHit peptideHit : peptideSet) {
			
			// Gather protein taxonomy nodes
			List<TaxonomyNode> taxonNodes = new ArrayList<TaxonomyNode>();
			
			for (ProteinHit proteinHit : peptideHit.getProteinHits()) {
				taxonNodes.add(proteinHit.getTaxonomyNode());
			}
			
			// Find common ancestor node
			TaxonomyNode ancestor = taxonNodes.get(0);
			for (int i = 0; i < taxonNodes.size(); i++) {
					ancestor = getCommonTaxonomyNode(ancestor, taxonNodes.get(i));
			}

			// Gets the parent node of the taxon node
			TaxonomyNode child = ancestor;
			TaxonomyNode parent = nodeMap.get(ancestor.getId());
			if (parent == null) {
				parent = child.getParentNode();
				while (true) {
					TaxonomyNode temp = nodeMap.get(parent.getId());
				
					if (temp == null) {
						child.setParentNode(parent);
						nodeMap.put(parent.getId(), parent);
						child = parent;
						parent = parent.getParentNode();
					} else {
						child.setParentNode(temp);
						break;
					}
				}
			} else {
				ancestor = parent;
			}

			// set peptide hit taxon node to ancestor
			peptideHit.setTaxonomyNode(ancestor);

			// possible TODO: determine spectrum taxonomy instead of inheriting directly from peptide
			for (SpectrumMatch match : peptideHit.getSpectrumMatches()) {
				match.setTaxonomyNode(ancestor);
			}
			// fire progress notification
			Client.getInstance().firePropertyChange("progressmade", false, true);
		}
	}
	
	/**
	 * Method to set taxonomy of a protein from common taxonomies of its peptides.
	 * @param proteinList List of proteins hits.
	 * @throws Exception
	 */
	public static void retrieveTaxonomyByPeptideTaxonomies(ProteinHitList proteinList) throws Exception {
		for (ProteinHit proteinHit : proteinList) {
			// gather protein taxonomy nodes
			List<TaxonomyNode> taxonNodes = new ArrayList<TaxonomyNode>();
			for (PeptideHit peptideHit : proteinHit.getPeptideHitList()) {
				taxonNodes.add(peptideHit.getTaxonomyNode());
			}
			// find common ancestor node
			TaxonomyNode ancestor = taxonNodes.get(0);
			for (int i = 1; i < taxonNodes.size(); i++) {
				ancestor = TaxonomyUtils.getCommonTaxonomyNode(ancestor, taxonNodes.get(i));
			}
			// set peptide hit taxon node to ancestor
			proteinHit.setTaxonomyNode(ancestor);
			// fire progress notification
			Client.getInstance().firePropertyChange("progressmade", false, true);
		}
	}

	/**
	 * Gets the tax name by the rank from the NCBI taxonomy.
	 * @param proteinHit Protein hit
	 * @param taxRank The taxonomic rank
	 * @return The name of the taxonomy.
	 */
	public static String getTaxNameByRank(TaxonomyNode taxNode, TaxonomyRank taxRank) {
		// Default value for taxonomy name.
		String taxName = "Unclassified";

		while (taxNode.getId() != 1) { // unequal to root
			if (taxNode.getRank().equals(taxRank.toString().toLowerCase())) {
				taxName = taxNode.getName();
				break;
			}
			taxNode = taxNode.getParentNode();
		}

		return taxName; 
	}
	
	/**
	 * Method to check whether a taxonomy belongs to a certain group determined by a certain NCBI taxonomy number.
	 * @param taxNode. Taxonomy node.
	 * @param filterTaxId. NCBI taxonomy ID.
	 * @return belongs to ? true / false
	 */
	public static boolean belongsToGroup(TaxonomyNode taxNode, long filterTaxId) {
		// Does not belong to group.
		boolean belongsToGroup = false;
		
		// To care for same taxID and especially for root as filtering level.
		if (filterTaxId == taxNode.getId()) { 
			belongsToGroup = true;
		} else {
			// Get all parents of the taxonNode and check whether they are equal to the filter level.
			while (taxNode.getParentNode() != null || (taxNode.getId() != 1)) {
				// Get parent taxon node of protein entry.
				try {
					taxNode = taxNode.getParentNode();
				} catch (Exception e) {
					e.printStackTrace();
				}
				// Check for filter ID
				if (filterTaxId == taxNode.getId()) {
					belongsToGroup = true;
					break;
				}
			}
		}
		return belongsToGroup;
	}

	// TODO: Probably most of the legacy code from NcbiTaxonomy class can be removed (see below)...  
	
//	/**
//	 * Returns a parent taxonomy node of the specified child taxonomy node.
//	 * @param childNode the child node
//	 * @return a parent node
//	 * @throws Exception if an I/O error occurs
//	 */
//	synchronized public TaxonomyNode getParentTaxonomyNode(TaxonomyNode childNode) throws Exception {
//		return this.getParentTaxonomyNode(childNode, true);
//	}

//	/**
//	 * Returns a parent taxonomy node of the specified child taxonomy node. The
//	 * parent node's rank may be forced to conform to the list of known ranks.
//	 * @param childNode the child node
//	 * @param knownRanksOnly <code>true</code> if the parent node's rank shall be
//	 *  one of those specified in the list of known ranks, <code>false</code> otherwise
//	 * @return a parent node
//	 * @throws Exception if an I/O error occurs
//	 */
//	synchronized public TaxonomyNode getParentTaxonomyNode(TaxonomyNode childNode, boolean knownRanksOnly) throws Exception {
//
//		// Get parent data
//		int parentTaxId = this.getParentTaxId(childNode.getId());
//		String rank = this.getRank(parentTaxId);
//
//		if (knownRanksOnly) {
//			// As long as parent rank is not inside list of known ranks move up in taxonomic tree
//			while ((parentTaxId != 1) && !ranks.contains(rank)) {
//				parentTaxId = this.getParentTaxId(parentTaxId);
//				rank = this.getRank(parentTaxId);
//			}
//		}
//
//		// Wrap parent data in taxonomy node
//		return new TaxonomyNode(parentTaxId, rank, this.getTaxonName(parentTaxId));
//	}
//	/**
//	 * Returns the name of the taxonomy node belonging to the specified taxonomy id.
//	 * @param taxID the taxonomy id
//	 * @return the taxonomy node name
//	 * @throws Exception if an I/O error occurs
//	 */
//	synchronized public String getTaxonName(int taxID) throws Exception {
//
//		// Get mapping
//		int pos = namesMap.get(taxID);
//
//		// Skip to mapped byte position in names file
//		namesRaf.seek(pos);
//
//		// Read line and isolate second non-numeric value
//		String line = namesRaf.readLine();
//		line = line.substring(line.indexOf("\t|\t") + 3);
//		line = line.substring(0, line.indexOf("\t"));
//
//		return line;
//	}
//
//	/**
//	 * Returns the parent taxonomy id of the taxonomy node belonging to the specified taxonomy id.
//	 * @param taxId the taxonomy id
//	 * @return the parent taxonomy id
//	 * @throws Exception if an I/O error occurs
//	 */
//	synchronized public int getParentTaxId(int taxId) throws Exception {
//
//		// Get mapping
//		int pos = nodesMap.get(taxId);
//
//		// Skip to mapped byte position in nodes file
//		nodesRaf.seek(pos);
//
//		// Read line and isolate second numeric value
//		String line = nodesRaf.readLine();
//		line = line.substring(line.indexOf("\t|\t") + 3);
//		line = line.substring(0, line.indexOf("\t"));
//		return Integer.valueOf(line).intValue();
//
//	}
//
//
//
//	/**
//	 * Returns the taxonomic rank identifier of the taxonomy node belonging to the specified taxonomy id.
//	 * @param taxID the taxonomy id
//	 * @return the taxonomic rank
//	 * @throws Exception if an I/O error occurs
//	 */
//	synchronized public String getRank(int taxID) throws Exception {
//
//		// Get mapping
//		int pos = nodesMap.get(taxID);
//
//		// Skip to mapped byte position in nodes file
//		nodesRaf.seek(pos);
//
//		// Read line and isolate third non-numeric value
//		String line = nodesRaf.readLine();
//		line = line.substring(line.indexOf("\t|\t") + 3);
//		line = line.substring(line.indexOf("\t|\t") + 3);
//		line = line.substring(0, line.indexOf("\t"));
//
//		return line;
//	}
//
//	/**
//	 * This method creates a TaxonNode for a certain taxID.
//	 * @param taxId the taxonomy id
//	 * @return The taxonNode containing the taxID, rank, taxName
//	 * @throws Exception if an I/O error occurs
//	 */
//	public TaxonomyNode createTaxonNode(int taxId) throws Exception {
//		TaxonomyNode taxNode = null;
//		taxNode = new TaxonomyNode(taxId,
//				this.getRank(taxId),
//				this.getTaxonName(taxId));
//		return taxNode;
//	}
//	
//	/**
//	 * Creates and returns the common taxonomy node above taxonomy nodes belonging to the
//	 * specified taxonomy IDs.
//	 * @param taxId1 the first taxonomy ID
//	 * @param taxId2 the second taxonomy ID
//	 * @return the common taxonomy node
//	 * @throws Exception 
//	 */
//	synchronized public TaxonomyNode createCommonTaxonomyNode(TaxonomyNode node1, Taxonomy node2) throws Exception {
//		return this.createTaxonNode(this.getCommonTaxonomyId(taxId1, taxId2));
//	}
//
//
//
//	/**
//	 * Finds the taxonomy level (taxID) were the 2 taxonomy levels intersect.
//	 * @param taxId1 NCBI taxonomy of the first entry
//	 * @param taxId2 NCBI taxonomy of the second entry
//	 * @return the NCBI taxonomy ID where both entries intersect (or 0 when something went wrong)
//	 * @throws Exception 
//	 */
//	public static Taxonomy getCommonTaxonomy(int taxId1, int taxId2) throws Exception {
//
//		// List of taxonomy entries for the first taxonomy entry.
//		List<Integer> taxList1 = new ArrayList<Integer>();
//		taxList1.add(taxId1);
//		while (taxId1 != 1) {	// 1 is the root node
//			try {
//				taxId1 = this.getParentTaxId(taxId1);
//				taxList1.add(taxId1);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//		// List of taxonomy entries for the second taxonomy entry.
//		List<Integer> taxList2 = new ArrayList<Integer>();
//		taxList2.add(taxId2);
//		while (taxId2 != 1) {	// 1 is the root node
//			try {
//				taxId2 = this.getParentTaxId(taxId2);
//				taxList2.add(taxId2);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//		// Get common ancestor
//		Integer taxId = 0;
//		for (int i = 0; i < taxList1.size(); i++) {
//			taxId = taxList1.get(i);
//			if (taxList2.contains(taxId)) {
//				break;
//			}
//		}
//
//		// Find ancestor of closest known rank type
//		while (!ranks.contains(this.getRank(taxId)) && (taxId != 1)) {
//			taxId = this.getParentTaxId(taxId);
//		}
//
//		return taxId;
//	}
}	
