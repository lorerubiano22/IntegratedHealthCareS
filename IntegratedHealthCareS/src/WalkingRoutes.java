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
	private double slack=0;
	private LinkedList<SubRoute> jobSlots; // list of subroutes which describe the job sequence respecting all restrictions
	private ArrayList<Jobs> jobList;
	private HashMap<Integer, Jobs> ServedJobs=new HashMap<> ();
	private Inputs inp; // input problem
	private Test test; // input problem
	private Random rn;


	// Methods
	public WalkingRoutes(Inputs input, Random r, Test t, List<Jobs> nodes) {
		jobList= new ArrayList<Jobs>();
		inp=input;
		test=t;
		for(Jobs i: nodes) {
			if(i.getReqQualification()!=0) {
				i.setStartServiceTime(i.getStartTime()); // the start time of the service  is fixed as the earliest time
				//i.setserviceTime(i.getEndTime()); // the start time of the service is fixed as the latest time
				jobList.add(i);
			}
		}

		jobList.sort(Jobs.TWSIZE_Early); // sorting list of jobs according the earliest time and the size of the TW
		jobSlots= new LinkedList<SubRoute>();

		String[] serviceStartTime= new String[3]; //Criteria for setting the service start time
		serviceStartTime[0] = "E"; // earliest time
		serviceStartTime[1] = "L";  // latest time
		serviceStartTime[2] = "R";  // random time

	
		creatingMultipleSlots(serviceStartTime);
		completingIndividualRoutes();
		//creatingOnlyOneSlotsSet(serviceStartTime);
		
		
		for(SubRoute wr:jobSlots) {
			if(wr.getJobSequence().size()>=1) {
				if(wr.getTotalTravelTime()==0) {
					wr.setTotalTravelTime(Double.MAX_VALUE); // High cost for slots with justn one job
				}
			}
		}
		System.out.println("Final slots");
		for(SubRoute wr:jobSlots) {
			System.out.print("\n Slot_Cost_ "+ wr.getTotalTravelTime());
			for(Jobs j:wr.getJobSequence()) {
				System.out.print(" j_( Id" + j.getId()+", B_"+j.getstartServiceTime()+") ");
			}
			System.out.print("\n");
		}
		ExactAllocation improveSlots= new ExactAllocation(test,inp);
		
		ExactAllocationDummyExample improveSlots1= new ExactAllocationDummyExample(test,inp);
		
		for(SubRoute wr:jobSlots) {
			System.out.print("\n Slot_Cost_ "+ wr.getTotalTravelTime());
			for(Jobs j:wr.getJobSequence()) {
				System.out.print(" j_( Id" + j.getId()+", B_"+j.getstartServiceTime()+") ");
			}
			System.out.print("\n");
		}

		//checkSequenceWalkingRoutes(); // remove repeated walking routes. If there two routes with the same jobs sequence the shortest one is selected
		improveSlots.selectionWalkingRoutes(jobSlots);
		improveSlots1.selectionWalkingRoutes(jobSlots);
		walkingRouteToJob(); // fix the pick-up and drop-off nodes for each walking route
		//		for(SubRoute wr:jobSlots) {
		//			System.out.println(wr.toString());
		//		}
	}



private void completingIndividualRoutes() {
	for(Jobs j:this.inp.getclients()) {
		this.ServedJobs.put(j.getId(),j);	
	}
		for(SubRoute slot:this.jobSlots) {
			if(slot.getJobSequence().size()==1) {
				if(ServedJobs.containsKey(slot.getJobSequence().getFirst().getId())) {
					ServedJobs.remove(slot.getJobSequence().getFirst().getId());
				}
			}
		}
		int slotId=jobSlots.size()-1;
		for(Jobs i:ServedJobs.values()) {
			SubRoute slot = new SubRoute();
			slotId++;
			slot.setSlotID(slotId);
			slot.addJobSequence(i,0,i.getstartServiceTime());
			jobSlots.add(slot);
		}
		
	}



//	private void creatingOnlyOneSlotsSet(String[] serviceStartTime) {
//		for(int b=0;b<serviceStartTime.length;b++) {
//			SubRoute wr = new SubRoute();
//			for(Jobs i:jobList) {
//				insertSequentially(i,wr,serviceStartTime[b]);
//				wr.updateInfWalkingRoute(inp);
//				System.out.println(wr.toString());
//			}
//			slotId++;
//			slot.setSlotID(slotId);
//			preliminarStartServiceTime(i,slot,jobPosition,serviceStartTime);
//			slot.addJobSequence(i,jobPosition,i.getstartServiceTime());
//			
//			
//		this.jobList
//		slotId++;
//		slot.setSlotID(slotId);
//		preliminarStartServiceTime(i,slot,jobPosition,serviceStartTime);
//		slot.addJobSequence(i,jobPosition,i.getstartServiceTime());
//		jobSlots.add(slot);
//			dummyWalkroute(serviceStartTime[i]);// one walking route per job
//			jobsInsertion(serviceStartTime[i]);
//		}
//	}



	private void insertSequentially(Jobs i, SubRoute wr, String string) {
		// TODO Auto-generated method stub
		
	}



	private void creatingMultipleSlots(String[] serviceStartTime) {
		for(int i=0;i<serviceStartTime.length;i++) {
			dummyWalkroute(serviceStartTime[i]);// one walking route per job
			jobsInsertion(serviceStartTime[i]);
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



	private void jobsInsertion(String serviceStartTime) {
		for(Jobs i:jobList) {
			for(SubRoute wr:jobSlots) {
				insertionJob(i,wr,serviceStartTime);
				wr.updateInfWalkingRoute(inp);
				System.out.println(wr.toString());
			}
		}
	}






	private void insertionJob(Jobs i, SubRoute wr, String serviceStartTime) {
		if(feasibleInsertion(i,wr)) { // it determines if the current route doesn't exceeds the maximum walking duration between two jobs
			// maximum walking duration between drop-off and delivery <- it is the route length
			if(serviceStartTime.equals("E")) {
				evaluateTheInsertionEarly(i,wr); // it evaluates and inserts the route if the insertion is feasible 
			}
			else {
					evaluateTheInsertionLatest(i,wr); // it evaluates and inserts the route if the insertion is feasible 
			}
		}
	}



	private void evaluateTheInsertionLatest(Jobs i, SubRoute wr) {
		if(!wr.getJobList().containsKey(i.getId())) {
			checkIfTheJobIsTheIntermediateJob(i,wr);  // try to insert the job before or after everything
		}
	}



	private void checkIfTheJobIsTheIntermediateJob(Jobs i, SubRoute wr) { // wr: {h,i,j,...}
		// Checking if is possible to insert before
		int estimatedB=0;
		int jobSequence=-1;
		for(Jobs j:wr.getJobSequence()) { // reading the list of jobs in the slot
			jobSequence++;
			if(wr.getJobSequence().getFirst().equals(j)) { // first job in the list
				estimatedB=j.getstartServiceTime()-i.getReqTime()-inp.getWalkCost().getCost(i.getId(),j.getId());
				if(estimatedB<=i.getEndTime() && estimatedB>=i.getStartTime()) {// check if it is possible insert the job before all jobs
					wr.addJobSequence(i,0,estimatedB);
				}
			}
			else {// in case that the job i could be an intermediate job or the last job
				if(!wr.getJobSequence().getLast().equals(j)) {// // in case that the job i could be an intermediate job
					int h=jobSequence-1;
					Jobs job_h=wr.getJobSequence().get(h); // calling the job h
					estimatedB=job_h.getstartServiceTime()+job_h.getReqTime()+inp.getWalkCost().getCost(job_h.getId(),i.getId());
					if(estimatedB>=i.getStartTime() && estimatedB<=i.getEndTime()){ // checking if from job j the new job i could be reachable
						if(estimatedB+i.getReqTime()+inp.getWalkCost().getCost(i.getId(),j.getId())<j.getstartServiceTime()) {
							wr.addJobSequence(i,jobSequence,estimatedB);
							System.out.println(wr.toString());
						}
					}	
				}
				else {// in case that the job i could be the final job
					if(wr.getJobSequence().getLast().equals(j)) {
						checkIfTheJobIsTheLatesestJob(i,wr);  // try to insert the job after everything
					}
				}
			}
			if(wr.getJobList().containsKey(i.getId())) {
				break;
			}
		}
	}



	private boolean feasibleInsertion(Jobs i, SubRoute wr) { // return true when is possible insert the job i, l. Otherwise return false
		boolean feasibility=false;
		boolean isNewJob=checkIsAnInsertedJob(i,wr); // it check if the job is already in the list
		if (isNewJob) {
			int slotSkill=wr.getSkill();
			if(slotSkill>=i.getReqQualification()) {
				// 1. Maximum working time (nurse)
				// 2. Maximum walking duration between two jobs
				// 3. Maximum walking duration between drop-off and delivery
				feasibility=checkWalkingDuration(i,wr);
			}
		}	
		return feasibility;
	}



	private void evaluateTheInsertionEarly(Jobs i, SubRoute wr) {
		// When the work is inserted before the other jobs it is 
		checkIfTheJobIsTheEarliestJob(i,wr);  // try to insert the job before everything
		if(!wr.getJobList().containsKey(i.getId())) {
			checkIfTheJobIsTheLatesestJob(i,wr);  // try to insert the job after everything
		}
	}

	private void checkIfTheJobIsTheLatesestJob(Jobs i, SubRoute wr) {
		Jobs firstJob=wr.getJobSequence().getLast();
		// 1. Assuming that the job could be assigned as the first job in the sequence and it could start the latest time
		int estimatedB= firstJob.getstartServiceTime()+firstJob.getReqTime()+inp.getWalkCost().getCost(firstJob.getId(),i.getId());
		if(estimatedB<=i.getEndTime()) { // The estimated time start for the service
			wr.addJobSequence(i,wr.getJobSequence().size(),estimatedB);
			System.out.println(wr.toString());
		}
	}



	private void checkIfTheJobIsTheEarliestJob(Jobs i, SubRoute wr) {
		// the inserted jobs are the reference to determine the start time for the incoming job 
		Jobs firstJob=wr.getJobSequence().getFirst();
		// 1. Assuming that the job could be assigned as the first job in the sequence and it could start the latest time
		if(i.getstartServiceTime()+i.getReqTime()+inp.getWalkCost().getCost(i.getId(),firstJob.getId())<=firstJob.getstartServiceTime()) {
			wr.addJobSequence(i,0,i.getStartTime());
		}
	}



	private boolean checkWalkingDuration(Jobs i, SubRoute wr) {
		boolean walkingDuration=false;
		Jobs lastJob=wr.getJobSequence().getLast();
		double travelTime=inp.getWalkCost().getCost(lastJob.getId(), i.getId());
		if(travelTime<=test.getWalking2jobs() && wr.getTotalTravelTime()<test.getCumulativeWalkingTime() && wr.getTotalTravelTime()<=test.getWorkingTime()) {
			walkingDuration=true;
		}
		return walkingDuration;
	}







	private boolean checkIsAnInsertedJob(Jobs i, SubRoute wr) {
		boolean isNewJob=false;
		if(!wr.getJobList().containsValue(i)) {
			isNewJob=true;
		}
		return isNewJob;
	}



	private void dummyWalkroute(String serviceStartTime) {
		int jobPosition =0;
		int slotId=jobSlots.size()-1;

		for(Jobs i:jobList) {
			SubRoute slot = new SubRoute();
			slotId++;
			slot.setSlotID(slotId);
			preliminarStartServiceTime(i,slot,jobPosition,serviceStartTime);
			slot.addJobSequence(i,jobPosition,i.getstartServiceTime());
			jobSlots.add(slot);
		}
	}



	private void preliminarStartServiceTime(Jobs i, SubRoute slot, int jobPosition, String serviceStartTime) {
		if(slot.getJobList().isEmpty()) {
			// assuming that the service will start in the latest time
			if(serviceStartTime.equals("E")) {
				i.setStartServiceTime(i.getStartTime());
			}
			else {
				if(serviceStartTime.equals("L")) {
					i.setStartServiceTime(i.getEndTime());
				}
				else {// random start service time
					rn = new Random(this.test.getSeed()); 
					int min=i.getStartTime();
					int max=i.getEndTime();
					int randomTime=rn.nextInt(max - min + 1) + min ;
					i.setStartServiceTime(randomTime);
				}
			}
		}

	}



	// Getters

	public double getTotalTravelTime() { return totalTravelTime;}
	public float getTotalServiceTime() {return totalServiceTime;}
	public double getSlack() {return slack;}
	public HashMap<Integer,Jobs> getServedJobs() {return ServedJobs;}


}
