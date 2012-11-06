package edu.bgsu.rna.jar3d;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import edu.bgsu.rna.jar3d.io.loaders.QueryLoader;
import edu.bgsu.rna.jar3d.io.loaders.QueryLoadingFailed;
import edu.bgsu.rna.jar3d.io.writers.ResultSaver;
import edu.bgsu.rna.jar3d.io.writers.SaveFailed;
import edu.bgsu.rna.jar3d.loop.Loop;
import edu.bgsu.rna.jar3d.loop.LoopType;
import edu.bgsu.rna.jar3d.query.Query;
import edu.bgsu.rna.jar3d.results.LoopResult;

/**
 * An Application is simple way to load sequences and models, run the models over the sequences and save the results.
 * This can only load from the file system currently and uses only bp models at the moment. In addition, it will only
 * run internal loops.
 */
public class Application {

	/** The query loader to use. */
	private final QueryLoader loader;

	/** The object to save results with. */
	private final ResultSaver saver;

	/** Default range limit */
	private static int RANGE_LIMIT = 20;

	/** Default model type */
	private static String MODEL_TYPE = "bp";

	/** Default model version */
	private static String VERSION = "0.6";

	/**
	 * Create a new Application.
	 * 
	 * @param loader The query loader to use.
	 * @param saver The results saver to use.
	 */
	public Application(QueryLoader loader, ResultSaver saver) {
		this.loader = loader;
		this.saver = saver;
	}

	/**
	 * Load then run a query and return the results.
	 * 
	 * @param queryId Query id to load.
	 * @param base Base path to the models.
	 * @return The results.
	 * @throws QueryLoadingFailed
	 */
	public List<List<LoopResult>> runQuery(String queryId, String base) throws QueryLoadingFailed {
		Query query = loader.load(queryId);
		return runQuery(query, base);
	}

	/**
	 * Run a query and return the results.
	 * 
	 * @param base The base bath to the models.
	 * @param query The query to run.
	 * @return The results.
	 */
	public List<List<LoopResult>> runQuery(Query query, String base) {
		List<List<LoopResult>> allResults = new ArrayList<List<LoopResult>>();
		for(Loop loop: query) {
			List<LoopResult> results = motifParse(base, loop); 
			allResults.add(results);
		}
		return allResults;
	}

	/**
	 * Run a loop against a single loop and return the results. This will only score internal loops, all other loop 
	 * types will give empty results.
	 * 
	 * @param base The base path to the models.
	 * @param loop The loop.
	 * @return Results of running the loop.
	 */
	private List<LoopResult> motifParse(String base, Loop loop) {
		List<LoopResult> result = new ArrayList<LoopResult>();

		String folder = base + File.separator + loop.getTypeString() + File.separator + VERSION;
		System.setProperty("user.dir", folder);

		Vector<String> modelNames = Sequence.getModelNames(folder, MODEL_TYPE, true);
		HashMap<String,MotifGroup> groupData = webJAR3D.loadMotifGroups(folder, MODEL_TYPE);

		if (loop.getLoopType() == LoopType.INTERNAL) {
			result = Alignment.doILdbQuery(loop, modelNames, groupData, RANGE_LIMIT);
		} else {
			result = new ArrayList<LoopResult>();
		}

		return result;
	}

	/**
	 * Save results using the ResultSaver. 
	 * 
	 * @param results The results to save.
	 * @throws SaveFailed
	 */
	public void saveResults(List<List<LoopResult>> results) throws SaveFailed {
		for(List<LoopResult> res: results) {
			saver.save(res);
		}
		saver.cleanUp();
	}

	/**
	 * Load a query, run the query and then save the results.
	 * 
	 * @param queryId Query to load.
	 * @param base Base path to the models.
	 * @throws SaveFailed
	 * @throws QueryLoadingFailed
	 */
	public void runAndSave(String queryId, String base) throws SaveFailed, QueryLoadingFailed {
		List<List<LoopResult>> results = this.runQuery(queryId, base);
		saver.writeHeader();
		saveResults(results);
	}
}
