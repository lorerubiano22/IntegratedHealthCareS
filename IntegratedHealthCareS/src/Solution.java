import java.util.LinkedList;



public class Solution {
	private static long nInstances = 0; // number of instances
	private long id; // solution ID
	private LinkedList<Route> routes; // list of routes in this solution
	private int passengers=0;// home care staff and + paramedic transported que salen del depot
	private double durationSolution = 0.0; // Travel distance = waiting time + driving time
	private double waitingTime=0;// Total waiting time
	private double serviceTime=0;
	private double drivingTime=0;
	private double walkingTime=0;
	private double paramedic=0;// los paramedicos que salen del depot
	private double homeCareStaff=0;// los paramedicos que salen del depot

	public Solution(Solution initialSol) {
		id=initialSol.id; // solution ID
		durationSolution = initialSol.durationSolution; // Travel distance = waiting time + driving time
		routes = copyRoutes(initialSol); // list of routes in this solution
		passengers=initialSol.passengers;// home care staff and + paramedic transported que salen del depot
		waitingTime=initialSol.waitingTime;// Total waiting time
		serviceTime=initialSol.serviceTime;
		drivingTime=initialSol.drivingTime;
		walkingTime=initialSol.walkingTime;
		paramedic=initialSol.paramedic;// los paramedicos que salen del depot
		homeCareStaff=initialSol.homeCareStaff;// los paramedicos que salen del depot
	}
	
	
	public Solution() {
		nInstances++;
        id = nInstances;
        routes = new LinkedList<Route>();
	}


	// Setters 
	public void setId(long id) {this.id = id;}
	public void setDurationSolution(double costs) {this.durationSolution = costs;}
	public void setRoutes(LinkedList<Route> routes) {this.routes = routes;}

	public void setPassengers(int p) {this.passengers = p;}

	public void setParamedic(double w) {paramedic=w;}
	public void setHomeCareStaff(double w) {homeCareStaff=w;}
	public void setWaitingTime(double w) {waitingTime=w;}
	public void setServiceTime(double s) {serviceTime=s;}
	public void setdrivingTime(double s) {drivingTime=s;}
	public void setWalkingTime(double w) {walkingTime=w;}

	// Getters
	public long getId() { return id;}
	public double getDurationSolution() { return durationSolution;}
	public int getPassengers() { return passengers;}
	public LinkedList<Route> getRoutes() {return routes;}
	public double getWaitingTime(){return waitingTime; }
	public double getServiceTime() {return serviceTime;}
	public double getdrivingTime(){return drivingTime;}
	public double getWalkingTime() {return walkingTime;}

	// auxiliar methods
	
	private LinkedList<Route> copyRoutes(Solution initialSol) {
		LinkedList<Route> copyRoutes= new LinkedList<Route>();
		for(Route r:initialSol.getRoutes()) {
			copyRoutes.add(new Route(r));
		}
		return copyRoutes;
	}
	
	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nID Solution: " + id);
	s = s.concat("\nFO: Travel time: " + durationSolution);
	s = s.concat("\nWalking time: " + walkingTime);
	s = s.concat("\n Total passengers: " + passengers);
	s = s.concat("\n Total paramedic: " + paramedic);
	s = s.concat("\n Total home care staff: " + homeCareStaff);
	s = s.concat("\n Waiting time: " + waitingTime);
	s= s.concat("\n Service time: "+  serviceTime);
	s = s.concat("\n List of jobs: ");
	for(Route r:routes) {
		if(!r.getSubJobsList().isEmpty()) {
			s= s.concat("\n Route: "+ r.getIdRoute());
			s= s.concat(" travelTime: "+ r.getTravelTime());
			s= s.concat(" waitingTime: "+ r.getWaitingTime());
			s= s.concat(" serviceTime: "+ r.getServiceTime());
			s= s.concat(" durationRoute: "+ r.getDurationRoute());
			s= s.concat("\n homeCareSaff amount: "+ r.getHomeCareStaff());
			s= s.concat(" paramedic amount: "+ r.getAmountParamedic());
			s= s.concat("\n");
			for(SubJobs j:r.getSubJobsList()) {	
				s = s.concat(" ( Id" + j.getSubJobKey()+", A_"+j.getArrivalTime()+", B_"+j.getstartServiceTime()+", reqTime_"+j.getReqTime()+") ");}
		}	
	}
	return s;
	}

}

