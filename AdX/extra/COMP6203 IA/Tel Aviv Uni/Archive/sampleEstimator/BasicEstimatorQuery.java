package sampleEstimator;

import static arch.AgentConstants.TAU_SIMDAYS;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import edu.umich.eecs.tac.props.Query;
import arch.AgentComponentQuery;

public class BasicEstimatorQuery extends AgentComponentQuery {


	/* estimated */
	public Double 	[]	estSales;
	public Double 	[]	estConvRate;
	public Double 	[]	estClickRate;

    /* actuals */
	public int 		[]	sales;
	public Double 	[]	convRate;
	public Double	[]	clickRate;
	public int 		[]	clicks;
	public Double   [] 	profitPerUnitSold;
	
	/* initial defaults */
    protected static double SALES_INIT = 10.0;
    protected static double CONVR_INIT = 0.3;
    protected static double CLKR_INIT = 0.3;

    /* logging */
	protected FileWriter outFileStats;
	protected PrintWriter outStats;

	
	public BasicEstimatorQuery(Query q) {
		super(q);
		estSales =	 	new Double[TAU_SIMDAYS+3];
		estConvRate =	new Double[TAU_SIMDAYS+3];
		estClickRate =  new Double[TAU_SIMDAYS+3];
		sales = 		new int[TAU_SIMDAYS+3];
		convRate = 		new Double[TAU_SIMDAYS+3];
		clicks = 		new int[TAU_SIMDAYS+3];
		clickRate = 	new Double[TAU_SIMDAYS+3];
		profitPerUnitSold =	new Double[TAU_SIMDAYS+3];
		
		try {
			outFileStats = new FileWriter(q.getManufacturer()+q.getComponent()+"_stats.txt");
			outStats = new PrintWriter(outFileStats);
		} catch (IOException e){
			e.printStackTrace();
		}
				
		nextDay(0);
	}
	
	/* prepare for the next day results 
	 * we initialize the estimates to default values
	 * @see arch.AgentComponentQuery#nextDay(int)
	 */
	public void nextDay(int day) {
		estSales[day]=SALES_INIT;
		estConvRate[day]=CONVR_INIT;
		estClickRate[day] = CLKR_INIT;
	}
	
	public void logStats(int day, double stat) {
		outStats.format("%d, %.2f%n",day,stat);
	}
	
	public void simulationFinished() {
		outStats.close();	
	}
	
}
