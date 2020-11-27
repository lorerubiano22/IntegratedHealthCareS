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
	private double idleTimeSol=0;  
	private double driverTimeSol=0;
	private double paramedic=0;// los paramedicos que salen del depot
	private double homeCareStaff=0;// los paramedicos que salen del depot
	private double driverCost=0;// los paramedicos que salen del depot
	private double homeCareStaffCost=0;// los paramedicos que salen del depot
	private double waitingTimeToPenalize=0; // 
	
	
	public Solution(Solution initialSol) {
		id=initialSol.id; // solution ID
		durationSolution = initialSol.durationSolution; // Travel distance = waiting time + driving time
		routes = new LinkedList<Route>();
		routes = copyRoutes(initialSol); // list of routes in this solution
		idleTimeSol=initialSol.idleTimeSol;
		passengers=initialSol.passengers;// home care staff and + paramedic transported que salen del depot
		waitingTime=initialSol.waitingTime;// Total waiting time
		driverCost=initialSol.driverCost;// los paramedicos que salen del depot
		homeCareStaffCost=initialSol.homeCareStaffCost;// los paramedicos que salen del depot
		serviceTime=initialSol.serviceTime;
		drivingTime=initialSol.drivingTime;
		walkingTime=initialSol.walkingTime;
		paramedic=initialSol.paramedic;// los paramedicos que salen del depot
		homeCareStaff=initialSol.homeCareStaff;// los paramedicos que salen del depot
		waitingTimeToPenalize=initialSol.waitingTimeToPenalize;
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
	public void setIdleTime(double idleTime) {idleTimeSol=	idleTime;}
	public void setObjFunctionDriver(double dr) {driverCost=	dr;}
	public void setObjFunctionHomeCareStaff(double dr) {homeCareStaffCost=	dr;}
	public void setwaitingTimeToPenalize(double dr) {waitingTimeToPenalize=	dr;}
	
	// Getters
	public long getId() { return id;}
	public double getDurationSolution() { return durationSolution;}
	public int getPassengers() { return passengers;}
	public LinkedList<Route> getRoutes() {return routes;}
	public double getWaitingTime(){return waitingTime; }
	public double getServiceTime() {return serviceTime;}
	public double getdrivingTime(){return drivingTime;}
	public double getWalkingTime() {return walkingTime;}
	public double getObjectiveFunctionDriver() {return driverCost;}
	public double getObjectiveFunctionHomeCareStaff() {return homeCareStaffCost;}
	public double getwaitingTimeToPenalize() {return waitingTimeToPenalize;}
	
	// auxiliar methods

	private LinkedList<Route> copyRoutes(Solution initialSol) {
		LinkedList<Route> copyRoutes= new LinkedList<Route>();
		// copy Routes
		for(Route r:initialSol.getRoutes()) {
			copyRoutes.add(new Route(r));
		}
		return copyRoutes;
	}

	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nID Solution: " + id);
	s = s.concat("\nFO: Duration: " + durationSolution);
	s = s.concat("\nFO: Travel time: " + drivingTime);
	s = s.concat("\nWalking time: " + walkingTime);
	s = s.concat("\n Total passengers: " + passengers);
	s = s.concat("\n Total paramedic: " + paramedic);
	s = s.concat("\n Total home care staff: " + homeCareStaff);
	s = s.concat("\n Idle time: " + idleTimeSol);
	s = s.concat("\n Waiting time: " + waitingTime);
	s= s.concat("\n waiting Time to penalize: "+ waitingTimeToPenalize);
	s = s.concat("\n Driver time: " + driverTimeSol);
	s= s.concat("\n Service time: "+  serviceTime);
	s = s.concat("\n List of jobs: ");
	for(Route r:routes) {
		if(!r.getSubJobsList().isEmpty()) {
			s= s.concat("\n Route: "+ r.getIdRoute());
			s= s.concat(" travelTime: "+ r.getTravelTime());
			s= s.concat(" waitingTime: "+ r.getWaitingTime());
			s= s.concat(" serviceTime: "+ r.getServiceTime());
			s= s.concat(" waiting Time to penalize: "+ r.getwaitingTimeToPenalize());
			s= s.concat(" IdleTime: "+ r.getIdleTime());
			s= s.concat(" durationRoute: "+ r.getDurationRoute());
			s= s.concat("\n homeCareSaff amount: "+ r.getHomeCareStaff());
			s= s.concat(" paramedic amount: "+ r.getAmountParamedic());
			s= s.concat("\n");
			for(Parts p:r.getPartsRoute()) {
				for(SubJobs j:p.getListSubJobs()) {	
					s = s.concat(" ( " + j.getSubJobKey()+" A  "+j.getArrivalTime()+"  B  "+j.getstartServiceTime()+"   D  "+j.getDepartureTime()+"  reqTime_"+j.getReqTime()+"  TW ["+j.getStartTime()+";"+j.getEndTime()+"]"+") \n");
				}
				s = s.concat("\n\n");
			}

		}	
	}
	return s;
	}





}

