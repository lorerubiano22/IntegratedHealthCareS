
public class Test {
	/* INSTANCE FIELDS AND CONSTRUCTOR */
    private final String instanceName;
    private final int testTime; // Max. cost allowed for any single route
    private final long seed; // Seed value for the Random Number Generator
    
    public Test(String name, int maxTime, long seed)
    {
		instanceName = name;
        testTime = maxTime*60;
        this.seed = seed;
    }
    
    /* GET METHODS */
    public long getSeed(){return seed;}
    public int getTestTime() { return testTime; }
    public String getInstanceName() { return instanceName;}

}
