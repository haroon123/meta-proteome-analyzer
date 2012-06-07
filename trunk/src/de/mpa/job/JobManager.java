package de.mpa.job;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;

import de.mpa.job.instances.DeleteJob;
import de.mpa.job.instances.MS2FormatJob;
import de.mpa.job.instances.RenameJob;


/**
 * The JobManager handles the execution of the various jobs.
 * @author Thilo Muth
 *
 */
public class JobManager {
	
	/**
	 * JobManager instance.
	 */
	private static JobManager instance;

	/**
	 * Logger instance.
	 */
	private Logger log = Logger.getLogger(JobManager.class);
	
	/**
	 * JobQueue instance.
	 */
	private Queue<Job> jobQueue;
	
	/**
	 * List of objects from the various
	 */
	private List<Object> objects;
	
	/**
	 * Constructor for the job manager.
	 */
	private JobManager() {
		this.jobQueue = new ArrayDeque<Job>();
	}
	
	/**
	 * Returns the JobManager instance.
	 *
	 * @return the JobManager instance
	 */
	public static JobManager getInstance() {
		if (instance == null) {
			instance = new JobManager();
		}
		return instance;
	}
	
	/**
	 * Adds a job to the job queue.
	 * @param job
	 */
	public void addJob(Job job){
		jobQueue.add(job);
	}
	
	/**
	 * Removes a job from the job queue.
	 * @param job
	 */
	public void deleteJob(Job job){
		jobQueue.remove(job);
	}
	
	/**
	 * Executes the jobs from the queue.
	 */
	public void execute() {
		// Iterate the job queue
		for(Job job : jobQueue) {
			if(job instanceof MS2FormatJob) {
				MS2FormatJob ms2formatjob = (MS2FormatJob) job;
				ms2formatjob.run();
			} else if (job instanceof DeleteJob) {
				DeleteJob deletejob = (DeleteJob) job;
				deletejob.execute();
			} else if (job instanceof RenameJob) {
				RenameJob renameJob = (RenameJob) job;
				renameJob.execute();
			} else {
				// Set the job status to RUNNING and put the message in the queue
				log.info("Executing job: " + job.getDescription());
				job.execute();
			}
			
			// Remove job from the queue after successful execution.
			jobQueue.remove(job);
		}
	}
    
	/**
	 * Returns a list of objects.
	 * @return
	 */
	public List<Object> getObjects(){
		return objects;
	}
	
	/**
	 * This method deletes all the jobs from the queue.	
	 */	
	public void clear(){		
		jobQueue.clear();
	}
	
}
