package de.mpa.client.ui.chart;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class IdentificationData implements ChartData {
	
	/**
	 * IdentificationData constructor. 
	 * @param TODO: Add parameters!
	 */
	public IdentificationData() {
		//this.occurrencesMap = occurrencesMap;
	}
	
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Returns the category dataset.
	 * @return Category dataset.
	 */
	public CategoryDataset getDataset() {
		DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
		// TODO: Add the default category dataset.
		return categoryDataset;
	}
	
	public void clear() {
		
	}

}