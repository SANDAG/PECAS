package com.hbaspecto.pecas.aa;

import com.pb.models.utils.StatusLogger;

/**
 * This class passes status logging messages to the StatusLogger class, but only if status logging is enabled.
 * Status logging is disabled by default.
 * @author HBA
 */
public class AAStatusLogger {
	private static boolean enabled = false;
	private static boolean inSubgoal = false;
	private static double subgoalStart;
	private static double subgoalScale;
	private static String moduleName = "default";
	
	/**
	 * Ensures that status logging is enabled.
	 */
	public static void enable() {
		enabled = true;
	}
	
	/**
	 * Ensures that status logging is disabled.
	 */
	public static void disable() {
		enabled = false;
	}
	
	/**
	 * Set the name of the module for all logging statements.
	 * @param module The name of the module.
	 */
	public static void setModule(String module)
	{
		moduleName = module;
	}
	
	/**
     * Convenience method to log a text status message with a default title.  The title is formed by taking the first
     * part of the module name (the text up to the first dot (".")), capitalizing it, and appending " Status" to it.
     *
     * @param module
     *        The module logging this message.  Should be in the form <code>module.submodule.subsubmodule</code>(etc.).
     *        An example would be <code>pt.ld</code>
     *
     * @param text
     *        The status message to log.
     */
	public static void logText(String text) {
		if(enabled)
			StatusLogger.logText(moduleName, text);
	}
	
	/**
     * Log a histogram status message.  A histogram message can be looked at as an "amount complete" gauge; it essentially
     * reports how much has been completed, and how much will have been completed when the model/module is finished.  As
     * the logger semicolon delimits its message (for whatever processor may use it), semicolons should not be used in
     * any of the arguments in the message.
     *
     * @param module
     *        The module logging this message.  Should be in the form <code>module.submodule.subsubmodule</code>(etc.).
     *        An example would be <code>pt.ld</code>
     *
     * @param title
     *        The title to use for this status entry.
     *
     * @param goal
     *        The point at which the model/module will be finished.
     *
     * @param currentPoint
     *        The current point.
     *
     * @param xAxisLabel
     *        The label to use for the histogram's x-axis.
     *
     * @param yAxisLabel
     *        The label to use for the histogram's y-axis.
     */
	public static void logHistogram(String title, double goal, double currentPoint, String xAxisLabel, String yAxisLabel) {
		if(enabled)
			StatusLogger.logHistogram(moduleName, title, goal, currentPoint, xAxisLabel, yAxisLabel);
	}
	
	/**
     * Log a graph status message. This consists primarily of a pair of points (x,y) representing the current status of
     * the model/module.  A collection of these points could be read by the processor and graphed to show both the
     * current status as well as its progression. As the logger semicolon delimits its message (for whatever processor
     * may use it), semicolons should not be used in any of the arguments in the message.
     *
     * @param module
     *        The module logging this message.  Should be in the form <code>module.submodule.subsubmodule</code>(etc.).
     *        An example would be <code>pt.ld</code>
     *
     * @param title
     *        The title to use for this status entry.
     *
     * @param xPoint
     *        The x coordinate of the point to be graphed.
     *
     * @param yPoint
     *        The y coordinate of the point to be graphed.
     *
     * @param xAxisLabel
     *        The label to use for the histogram's x-axis.
     *
     * @param yAxisLabel
     *        The label to use for the histogram's y-axis.
     */
	public static void logGraph(String title, double xPoint, double yPoint, String xAxisLabel, String yAxisLabel) {
		if(enabled)
			StatusLogger.logGraph(moduleName, title, xPoint, yPoint, xAxisLabel, yAxisLabel);
	}
}
