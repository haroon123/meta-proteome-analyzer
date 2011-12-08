package de.mpa.db.accessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class XTandemhit extends XtandemhitTableAccessor {
	private String sequence = null;
	private String accession = null;
	
    /**
     * This constructor reads the spectrum file from a resultset. The ResultSet should be positioned such that a single
     * row can be read directly (i.e., without calling the 'next()' method on the ResultSet).
     *
     * @param aRS ResultSet to read the data from.
     * @throws SQLException when reading the ResultSet failed.
     */
    public XTandemhit(ResultSet aRS) throws SQLException {
        super(aRS);
        this.sequence = (String) aRS.getObject("sequence");
        this.accession = (String) aRS.getObject("accession");
    }
    
    /**
     * This method will find the hits from the current connection, based on the specified spectrumid.
     *
     * @param aSpectrumID long with the spectrumid of the spectrum file to find.
     * @param aConn           Connection to read the spectrum File from.
     * @return Spectrumfile with the data.
     * @throws SQLException when the retrieval did not succeed.
     */
    public static List<XTandemhit> getHitsFromSpectrumID(long aSpectrumID, Connection aConn) throws SQLException {
    	List<XTandemhit> temp = new ArrayList<XTandemhit>();
        PreparedStatement ps = aConn.prepareStatement("select x.*, p.sequence, pr.accession from xtandemhit x, peptide p, protein pr, pep2prot p2p where x.fk_peptideid = p.peptideid and x.fk_peptideid = p2p.fk_peptideid and p2p.fk_proteinsid = pr.proteinid and x.fk_spectrumid = ?");
        ps.setLong(1, aSpectrumID);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            XTandemhit hit = new XTandemhit(rs);
            temp.add(hit);	
        }
        rs.close();
        ps.close();
        return temp;
    }

	public String getSequence() {
		return sequence;
	}

	public String getAccession() {
		return accession;
	}
}
