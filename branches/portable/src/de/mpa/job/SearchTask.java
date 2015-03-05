package de.mpa.job;

import java.io.File;
import java.util.List;

import de.mpa.client.DbSearchSettings;
import de.mpa.job.instances.OmssaJob;
import de.mpa.job.instances.XTandemJob;
import de.mpa.job.scoring.OmssaScoreJob;
import de.mpa.job.scoring.XTandemScoreJob;

public class SearchTask {
    
	/**
	 * List of MGF Files.
	 */
	private List<File> mgfFiles;
	
	/**
	 * Database search settings.
	 */
	private DbSearchSettings searchSettings;
    
    /**
     * Runs the searches in one task.
     * @param mgfFiles
     * @param dbSearchSettings
     */
	public SearchTask(List<File> mgfFiles, DbSearchSettings dbSearchSettings) {
		this.mgfFiles = mgfFiles;
		this.searchSettings = dbSearchSettings;
		init();
	}
	
	/**
	 * Initializes the task.
	 */
	private void init() {
		JobManager jobManager = JobManager.getInstance();
		
		// Iterate the MGF files.
		for (File mgfFile : mgfFiles) {
			// X!Tandem job
			if (searchSettings.isXTandem()) {
				searchSettings.setSearchType(SearchType.TARGET);
				Job xtandemTargetJob = new XTandemJob(mgfFile, searchSettings);
				jobManager.addJob(xtandemTargetJob);
				
				searchSettings.setSearchType(SearchType.DECOY);
				Job xtandemDecoyJob = new XTandemJob(mgfFile, searchSettings);
				jobManager.addJob(xtandemDecoyJob);
				
				// The score job evaluates X!Tandem target + decoy results
				Job xTandemScoreJob = new XTandemScoreJob(xtandemTargetJob.getFilename(), xtandemDecoyJob.getFilename());
				jobManager.addJob(xTandemScoreJob);
			}
			
			// OMSSA job
			if (searchSettings.isOmssa()) {
				searchSettings.setSearchType(SearchType.TARGET);
				Job omssaTargetJob = new OmssaJob(mgfFile, searchSettings);
				jobManager.addJob(omssaTargetJob);
				
				searchSettings.setSearchType(SearchType.DECOY);
				Job omssaDecoyJob = new OmssaJob(mgfFile, searchSettings);
				jobManager.addJob(omssaDecoyJob);
				
				// The score job evaluates X!Tandem target + decoy results
				Job xTandemScoreJob = new OmssaScoreJob(omssaTargetJob.getFilename(), omssaDecoyJob.getFilename());
				jobManager.addJob(xTandemScoreJob);
			}
		}	
	}

}
