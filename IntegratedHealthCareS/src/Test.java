
public class Test {
	/* INSTANCE FIELDS AND CONSTRUCTOR */
	private final String instanceName;
	private final int testTime; // Max. cost allowed for any single route
	private final long seed; // Seed value for the Random Number Generator
	private final double workingTime;

	private final double walking2jobs;
	private final double detour;
	private final double cumulativeWalkingTime;
	private final double cumulativeWaitingTime;

	public Test(String name, int maxTime, long seed, double workingTime, double walking2jobs, double detour, double cumulativeWalkingTime, double cumulativeWaitingTime)
	{
		instanceName = name;
		testTime = maxTime*60;
		this.seed = seed;
		this.workingTime=workingTime;
		this.walking2jobs=walking2jobs;
		this.detour=detour;
		this.cumulativeWalkingTime=cumulativeWalkingTime;
		this.cumulativeWaitingTime=cumulativeWaitingTime;
	}

	/* GET METHODS */
	public long getSeed(){return seed;}
	public int getTestTime() { return testTime; }
	public String getInstanceName() { return instanceName;}
	public double getWorkingTime() {return workingTime;}
	public double getWalking2jobs() {return walking2jobs;}
	public double getDetour() {return detour;}
	public double getCumulativeWalkingTime() {return cumulativeWalkingTime;}
	public double getCumulativeWaitingTime() {return cumulativeWaitingTime;}

}
