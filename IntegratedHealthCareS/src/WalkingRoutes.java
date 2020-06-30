import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


public class WalkingRoutes {
	// Walking route attributes:+
	// list of edges
	// list of jobs
	// walking time
	// pick-up Node
	// drop-off node

	private double totalTravelTime = 0.0; // route total travel time
	private float totalServiceTime = 0.0F; // route total service time in the route
	private LinkedList<SubRoute> jobSlots; // list of subroutes which describe the job sequence respecting all restrictions
	private ArrayList<Jobs> jobList;
	private ArrayList<Jobs> noServedJobs;
	private Inputs inp; // input problem
	private Test test; // input problem

	public WalkingRoutes(Inputs input, Random r, Test t, List<Jobs> nodes) {
		jobList= new ArrayList<Jobs>();
		noServedJobs= new ArrayList<Jobs>();
		inp=input;
		test=t;
		for(Jobs i: nodes) {
			jobList.add(i);
		}
		jobSlots= new LinkedList<SubRoute>();
		dummyWalkroute();// one walk route per job
		jobsInsertion();

		for(SubRoute wr:jobSlots) {
			System.out.println(wr.toString());
		}
		checkSequenceWalkingRoutes(); // remove repeated walking routes. If there two routes with the same jobs sequence the shortest one is selected

		walkingRouteToJob(); // fix the pick-up and drop-off nodes for each walking route

		for(SubRoute wr:jobSlots) {
			System.out.println(wr.toString());
		}
	}



	private void walkingRouteToJob() {
		for(SubRoute wr:jobSlots) {
			wr.setPickUpNode(wr.getJobSequence().getFirst());
			wr.setDropOffNode(wr.getJobSequence().getLast());
		}

	}



	private void checkSequenceWalkingRoutes() {
	HashMap<Integer,SubRoute> jobSlotsCOPY=new HashMap<> ();
	int route=-1;
		for(SubRoute wr:jobSlots) {
			if(wr.getJobSequence().size()>1) {
				route++;
				wr.setSlotID(route);
				jobSlotsCOPY.put(wr.getSlotID(), wr);}
		}

		for(SubRoute wr0:jobSlots) {
			for(SubRoute wr1:jobSlots) {
				if(jobSlotsCOPY.containsValue(wr0) && jobSlotsCOPY.containsValue(wr1)) {
					if(!wr0.equals(wr1)) {
						boolean StartEndJob= areTheyTheSameJob(wr0,wr1);
						if(StartEndJob) {
							removeLongerRoute(wr0,wr1,jobSlotsCOPY);
							// TO DO: check the route with the tighter TW
						}
					}
				}
			}	
		}
		jobSlots.clear();
		System.out.println("AFTER CLEANING");
		for(SubRoute wr:jobSlots) {
			System.out.println(wr.toString());
		}
		for(SubRoute wr:jobSlotsCOPY.values()) {
			jobSlots.add(wr);
			wr.updateInfWalkingRoute(inp);
		}

	}



	private void removeLongerRoute(SubRoute wr0, SubRoute wr1, HashMap<Integer, SubRoute> jobSlotsCOPY) {
	
		if(wr0.getdurationWalkingRoute()>wr1.getdurationWalkingRoute()) {
			jobSlotsCOPY.remove(wr0.getSlotID());
		}
		else {
			if(wr0.getdurationWalkingRoute()<wr1.getdurationWalkingRoute()) {
				jobSlotsCOPY.remove(wr1.getSlotID());
			}
			else { // are the same job sequence
				jobSlotsCOPY.remove(wr0.getSlotID());
			}
		}
	}



	private boolean areTheyTheSameJob(SubRoute wr0, SubRoute wr1) {
		boolean equalStartEndJob=true;
		if(wr0.getJobSequence().getFirst().equals(wr1.getJobSequence().getFirst())) {
			equalStartEndJob=true;
		}
		return equalStartEndJob;
	}



	private void jobsInsertion() {
		for(Jobs i:jobList) {
			for(SubRoute wr:jobSlots) {
				insertionJob(i,wr);
				wr.updateInfWalkingRoute(inp);
			}
		}
	}






	private void insertionJob(Jobs i, SubRoute wr) {
		if(feasibleInsertion(i,wr)) {
			wr.getJobSequence().add(i);
		}
	}



	private boolean feasibleInsertion(Jobs i, SubRoute wr) { // return true when is possible insert the job i. Otherwise return false
		boolean feasibility=false;
		// 1. only new jobs are inserted 
		boolean isNewJob=checkIsAnInsertedJob(i,wr); // it check if the job is already in the list
		if (isNewJob) {
			// 2.  maximum walking duration between two jobs
			// maximum walking duration between drop-off and delivery
			boolean walkingDuration=checkWalkingDuration(i,wr);
			if(walkingDuration) {
				System.out.println(checkJobTw(i,wr));
				if(checkJobTw(i,wr)) {// 2. time windows
					i.setserviceTime((int)wr.getdurationWalkingRoute());
					wr.addJobSequence(i);
				}
			}	
		}





		return feasibility;
	}



	private boolean checkWalkingDuration(Jobs i, SubRoute wr) {
		boolean walkingDuration=false;
		Jobs lastJob=wr.getJobSequence().getLast();
		double travelTime=inp.getWalkCost().getCost(lastJob.getId(), i.getId());
		if(travelTime<test.getWalking2jobs() && wr.getTotalTravelTime()<test.getCumulativeWalkingTime()) {
			walkingDuration=true;
		}
		return walkingDuration;
	}



	private boolean checkJobTw(Jobs i, SubRoute wr) {
		boolean withinTW = false;
		Jobs lastJob=wr.getJobSequence().getLast();
		int serviceTimeStart=lastJob.getstartServiceTime();
		double serviceTime=lastJob.getReqTime();
		double travelTime=inp.getWalkCost().getCost(lastJob.getId(), i.getId());
		double nextTW= serviceTimeStart+serviceTime+travelTime;
		if(nextTW>=i.getStartTime() && nextTW<=i.getEndTime()) {
			withinTW=true;
		}
		return withinTW;
	}



	private boolean checkIsAnInsertedJob(Jobs i, SubRoute wr) {
		boolean isNewJob=false;
		if(!wr.getJobList().containsValue(i)) {
			isNewJob=true;
		}
		return isNewJob;
	}



	private void dummyWalkroute() {
		for(Jobs i:jobList) {
			i.setserviceTime(i.getStartTime());
			SubRoute slot = new SubRoute(); 
			slot.addJobSequence(i);
			jobSlots.add(slot);
		}
	}


}
