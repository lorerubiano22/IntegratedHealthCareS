
public class Test {
	/* INSTANCE FIELDS AND CONSTRUCTOR */
	private final String instanceName;
	private final double testTime; // Max. cost allowed for any single route
	private final long seed; // Seed value for the Random Number Generator
	private final double workingTime;

	private final double walking2jobs;
	private final double detour;
	private final double cumulativeWalkingTime;
	private final int cumulativeWaitingTime;
	private final int loadTimePatient;
	private final int loadTimeHomeCareStaff;
	private final int patientRegistrationTime;

	public Test(String name, double maxTime, long seed, double workingTime, double walking2jobs, 
			double detour, double cumulativeWalkingTime, int cumulativeWaitingTime, int p,int HCS, int registrationTime){
		instanceName = name;
		testTime = maxTime*60;
		this.seed = seed;
		this.workingTime=workingTime;
		this.walking2jobs=walking2jobs;
		this.detour=detour;
		this.cumulativeWalkingTime=cumulativeWalkingTime;
		this.cumulativeWaitingTime=cumulativeWaitingTime;
		this.loadTimePatient= p; 
		this.loadTimeHomeCareStaff=HCS;
		this.patientRegistrationTime=registrationTime;
	}

	/* GET METHODS */
	public long getSeed(){return seed;}
	public double getTestTime() { return testTime; }
	public String getInstanceName() { return instanceName;}
	public double getWorkingTime() {return workingTime;}
	public double getWalking2jobs() {return walking2jobs;}
	public double getDetour() {return detour;}
	public double getCumulativeWalkingTime() {return cumulativeWalkingTime;}
	public int getCumulativeWaitingTime() {return cumulativeWaitingTime;}
	public int getloadTimePatient() {return loadTimePatient;}
	public int getloadTimeHomeCareStaff() {return loadTimeHomeCareStaff;}
	public int getRegistrationTime() {return patientRegistrationTime;}




}
