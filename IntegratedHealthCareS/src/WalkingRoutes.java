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
	private LinkedList<SubRoute> walkingRoutes; // list of selected walking routes
	private ArrayList<SubJobs> subJobList = new ArrayList<SubJobs>(); // this list contains 


	// Methods
	public WalkingRoutes(Inputs input, Random r, Test t, List<Jobs> nodes) {

		// Information
		jobList= new ArrayList<Jobs>();
		inp=input;
		test=t;
		for(Jobs i: inp.getclients().values()) {// List a client jobs
			if(i.getReqQualification()!=0) {
				i.setStartServiceTime(i.getStartTime()); // the start time of the service  is fixed as the earliest time
				jobList.add(i);
			}
		}

		// 1. sorting jobs
		//	jobList.sort(Jobs.TWSIZE_Early); // sorting list of jobs according the earliest time and the size of the TW
		jobList.sort(Jobs.SORT_BY_STARTW); // sorting list of jobs according the earliest time and the start time of the TW
		jobSlots= new LinkedList<SubRoute>();

		//2. Criteria for setting the service start time
		//		String[] serviceStartTime= new String[3]; 
		String[] serviceStartTime= new String[1];  // to remove
		serviceStartTime[0] = "L"; // earliest time
		//		serviceStartTime[1] = "L";  // latest time
		//		serviceStartTime[2] = "R";  // random time


		//3. Creating multiples slots - set covering problem
		creatingMultipleSlots(serviceStartTime);
		settingCostIndividualRoutes();
		completingIndividualRoutes();

		// 5. To Do: Improvement for slots   Method to improve the slots
		//??? PERT!!

		// 6.1 Computing walking cost



		// 6.2 To Do: Computing walking time duration: Method to determine the start time for each job in the walking route - apply PERT method for each slot
		computingWalkingTimeDuration(jobSlots); // computing the travel time for each slot walking time + waiting time

		System.out.println("potential WR");
		for(SubRoute wr:jobSlots) {
			//if(wr.getJobSequence().size()>1) {
			System.out.print("\n WR_Cost_ iD  "+wr.getSlotID()+" "+ wr.getTotalTravelTime());  // to remove
			for(Jobs j:wr.getJobSequence()) {
				System.out.print(" j_( Id" + j.getId()+", B_"+j.getstartServiceTime()+") "); // to remove
			}
			System.out.print("\n");
			//}
		}
		// 7. Solving set partitioning problem
		ExactAllocation improveSlots= new ExactAllocation(test,inp);
		improveSlots.selectionWalkingRoutes(jobSlots);
		walkingRoutes=improveSlots.getWalkingRoutes();


		System.out.println("\n Final WR");

		for(SubRoute wr:walkingRoutes) {
			if(wr.getJobSequence().size()>1) {
				Jobs dropOff=wr.getJobSequence().getFirst();
				Jobs pickUp=wr.getJobSequence().getLast();
				dropOff.setPair(pickUp);  // presentJob.setPair(futureJob); 
				wr.setDropOffNode(dropOff);
				wr.setPickUpNode(pickUp);

				System.out.print("\n WR_Cost_ "+ wr.getDurationWalkingRoute());
				for(Jobs j:wr.getJobSequence()) {
					System.out.print("\n j_( Id" + j.getId()+" TW "+j.getStartTime()+"   "+j.getEndTime()+"   , B_"+j.getstartServiceTime()+") ");
				}
				System.out.print("\n");
			}
		}

		// checkSolutionQuality <- each job is once in the set of solutions
		boolean goodSolution = checkWalkingRouteSet();
		System.out.print("\n good Solution" + goodSolution);
		// 8. Making walking routes into big tasks 
		walkingRouteToJob(); // fix the pick-up and drop-off nodes for each walking route
	}



	private void settingCostIndividualRoutes() {
		for(SubRoute slot:jobSlots) {
			if(slot.getJobSequence().size()==1) {
				Jobs i=slot.getJobSequence().getFirst();
				slot.setTotalServiceTime(i.getReqTime());
				double fixValue=1000000;
				slot.setTotalTravelTime(fixValue);
				slot.setDurationWalkingRoute(slot.getTotalServiceTime()+fixValue);
			}
		}

	}



	private boolean checkWalkingRouteSet() {
		boolean goodSolution= true;
		HashMap<Integer,Jobs> validation = new HashMap<Integer,Jobs>();
		for(SubRoute wr:walkingRoutes) {
			for(Jobs j:wr.getJobSequence()) {
				if(!validation.containsKey(j.getId())) {
					validation.put(j.getId(), j);
				}
				else {
					goodSolution=false;
				}
			}
			if(!goodSolution) {
				break;
			}
		}
		return goodSolution;
	}



	private void computingWalkingTimeDuration(LinkedList<SubRoute> jobSlots2) {
		//1 Computing the waiting time for each job in each slot
		for(SubRoute wr:jobSlots2) {
			double waitingTime=0;
			double serviceTime=0;
			for(Jobs j: wr.getJobSequence()) {
				j.setWaitingTime(j.getStartTime(), j.getstartServiceTime());
				waitingTime+=j.getWaitingTime();
				serviceTime+=j.getReqTime();
			}
			wr.setwaitingTimeRoute(waitingTime);
			wr.setTotalServiceTime(serviceTime);
			wr.setDurationWalkingRoute(waitingTime+wr.getTotalTravelTime()+wr.getTotalServiceTime());
		}
	}



	private void completingIndividualRoutes() {
		for(Jobs j:this.inp.getclients().values()) {
			this.ServedJobs.put(j.getId(),j);	
		}
		//		for(SubRoute slot:this.jobSlots) {
		//			if(slot.getJobSequence().size()==1) {
		//				if(ServedJobs.containsKey(slot.getJobSequence().getFirst().getId())) {
		//					ServedJobs.remove(slot.getJobSequence().getFirst().getId());
		//				}
		//			}
		//		}
		int slotId=jobSlots.size()-1;
		for(Jobs job:this.inp.getclients().values()) {
			Jobs i=new Jobs(job);
			SubRoute slot = new SubRoute();
			slot.setTotalServiceTime(i.getReqTime());
			double fixValue=1000000;
			slot.setTotalTravelTime(fixValue);
			slot.setDurationWalkingRoute(slot.getTotalServiceTime()+fixValue);
			slotId++;
			slot.setSlotID(slotId);
			slot.addJobSequence(i,0,i.getstartServiceTime());
			jobSlots.add(slot);
		}

	}




	private void creatingMultipleSlots(String[] serviceStartTime) {
		for(int i=0;i<serviceStartTime.length;i++) {
			dummyWalkroute(serviceStartTime[i]);// one walking route per job
			jobsInsertion(serviceStartTime[i]);
		}
	}



	private void walkingRouteToJob() {
		for(SubRoute wr:jobSlots) {
			if(wr.getJobSequence().size()>1) {// only for real walking routes
				wr.setDropOffNode(wr.getJobSequence().getFirst());

				wr.setPickUpNode(wr.getJobSequence().getLast());

				/*
				 *  Present job
				 */
				Jobs presentbigJob=new Jobs(wr.getDropOffNode());
				// 1. Setting the TW and start service time
				double startTime=wr.getDropOffNode().getstartServiceTime()-test.getloadTimeHomeCareStaff(); // considering the unloading of the home health care staff
				presentbigJob.setStartTime(startTime);
				presentbigJob.setEndTime(startTime);
				presentbigJob.setStartServiceTime(wr.getDropOffNode().getstartServiceTime());
				// 2. Setting the duration of the jobs
				presentbigJob.setserviceTime(wr.getDurationWalkingRoute()); 
				// 3. Setting qualification of the nurse
				presentbigJob.setReqQualification(wr.getSkill());

				/*
				 *  Future job
				 */
				Jobs futurebigJob=new Jobs(wr.getPickUpNode());		
				// 1. Setting the TW and start service time
				futurebigJob.setStartTime((int)wr.getPickUpNode().getstartServiceTime()+ (int)wr.getPickUpNode().getReqTime());
				futurebigJob.setEndTime((int)futurebigJob.getStartTime()+ (int)this.test.getCumulativeWaitingTime());
				// 2. Setting the duration of the jobs
				futurebigJob.setserviceTime(test.getloadTimeHomeCareStaff());
				// 3. Setting qualification of the nurse
				SubJobs separateJobs= new SubJobs(presentbigJob,futurebigJob,wr);
				this.subJobList.add(separateJobs);
			}
		}
	}



	private void jobsInsertion(String serviceStartTime) {
		for(Jobs job:jobList) {


			for(SubRoute wr:jobSlots) {
				Jobs i=new Jobs(job);
				if(i.getId()==8) {
					System.out.print("Stop");
				}
				if(wr.getJobSequence().get(0).getId()==7 && wr.getJobSequence().get(0).getstartServiceTime()==wr.getJobSequence().get(0).getEndTime()) {
					if(i.getId()==11 || i.getId()==9 || i.getId()==8 || i.getId()==10 ) {
						System.out.println("\nStop\n");
					}
				}
				insertionJob(i,wr,serviceStartTime);
				//	wr.updateInfWalkingRoute(test,inp);
				System.out.println("\nRoute\n");
				System.out.println(wr.toString());
			}
		}
	}






	private void insertionJob(Jobs i, SubRoute wr, String serviceStartTime) {
		if(feasibleInsertion(i,wr)) { // it determines if the current route doesn't exceeds the maximum walking duration between two jobs
			// maximum walking duration between drop-off and delivery <- it is the route length
			if(feasibleTWAndWorkingTime(i,wr)) {
				i.setWaitingTime(i.getstartServiceTime(), i.getArrivalTime());
				wr.getJobSequence().add(i); // append job at the end of the sequence	
				computeRouteDurtion(wr);
			}

		}
	}




	public void computeRouteDurtion(SubRoute wr) {
		double service=0;
		double waitingTime=0;
		double travelTime=0;
		// service time and waiting time
		for(Jobs i:wr.getJobSequence()) {
			service+=i.getReqTime();
			waitingTime+=i.getWaitingTime();
		}
		wr.setwaitingTimeRoute(waitingTime);
		wr.setTotalServiceTime(service);
		// travel time
		for(int i =0;i<wr.getJobSequence().size()-1;i++) {
			int j=wr.getJobSequence().get(i).getId()-1;
			int k=wr.getJobSequence().get(i+1).getId()-1;
			double tv=inp.getWalkCost().getCost(j, k);
			travelTime+=tv;
		}
		wr.setTotalTravelTime(travelTime);
		double durationRoute=waitingTime+service+travelTime;
		wr.setDurationWalkingRoute(durationRoute);
		System.out.println("cost wr "+ wr.getDurationWalkingRoute());
	}



	private boolean feasibleTWAndWorkingTime(Jobs i, SubRoute wr) {
		boolean reacheable=false;
		Jobs lastJob=wr.getJobSequence().getLast();
		double travelTime=inp.getWalkCost().getCost(lastJob.getId()-1, i.getId()-1);
		double timeAtLastJob=lastJob.getstartServiceTime()+lastJob.getReqTime();
		double possibleArrivalTime= timeAtLastJob+travelTime ;
		//if(possibleArrivalTime>=i.getStartTime() && possibleArrivalTime<=i.getEndTime()) {
		if(possibleArrivalTime<=i.getEndTime()) {
			double waitingTime=0;
			if(possibleArrivalTime<i.getStartTime()) { // there is a waiting time
				waitingTime=i.getStartTime()-possibleArrivalTime;
			}
			double newJobTime=travelTime+waitingTime+i.getReqTime(); //1. Compute the time which involves the new inserted job: ArrivalTime+ tiemeReq + waiting time
			if(wr.getDurationWalkingRoute()+newJobTime<test.getWorkingTime()) { // if the new job doesn´t exceed the maximum working hours
				reacheable=true; // job i will be inserted in the Route wr
				i.setarrivalTime(possibleArrivalTime);
				double serviceStartTime=Math.max(i.getArrivalTime(), i.getStartTime());
				i.setStartServiceTime(serviceStartTime);
			}
		}
		return reacheable;
	}






	private void checkIfTheJobIsTheIntermediateJob(Jobs i, SubRoute wr) { // wr: {h,i,j,...}
		// Checking if is possible to insert before
		double estimatedB=0;
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





	private void checkIfTheJobIsTheLatesestJob(Jobs i, SubRoute wr) {
		Jobs firstJob=wr.getJobSequence().getLast();
		// 1. Assuming that the job could be assigned as the first job in the sequence and it could start the latest time
		double estimatedB= firstJob.getstartServiceTime()+firstJob.getReqTime()+inp.getWalkCost().getCost(firstJob.getId(),i.getId());
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
		double travelTime=inp.getWalkCost().getCost(lastJob.getId()-1, i.getId()-1);
		System.out.print("Travel Time " + travelTime);
		System.out.println("Cumulative " +(wr.getTotalTravelTime()+travelTime));
		if(travelTime<=test.getWalking2jobs() && wr.getTotalTravelTime()+travelTime<test.getCumulativeWalkingTime()) {
			walkingDuration=true;
		}
		return walkingDuration;
	}







	private boolean checkIsAnInsertedJob(Jobs i, SubRoute wr) {
		boolean isNewJob=false;
		for(Jobs j:wr.getJobSequence()) {
			wr.getJobList().put(j.getId(), j);
		}
		if(!wr.getJobList().containsKey(i.getId())) {
			isNewJob=true;
		}
		return isNewJob;
	}



	private void dummyWalkroute(String serviceStartTime) {
		int jobPosition =0;
		int slotId=jobSlots.size()-1;
		for(Jobs job:jobList) {
			Jobs i= new Jobs(job);
			SubRoute slot = new SubRoute();
			slotId++;
			slot.setSlotID(slotId);
			preliminarStartServiceTime(i,slot,jobPosition,serviceStartTime);
			slot.addJobSequence(i,jobPosition,i.getstartServiceTime());
			jobSlots.add(slot);
			System.out.println("\nRoute slot\n");
			System.out.println(slot.toString());
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
					double min=i.getStartTime();
					double max=i.getEndTime();
					double randomTime=rn.nextInt((int)max - (int)min + 1) + min ;
					i.setStartServiceTime(randomTime);
				}
			}
		}
		i.setarrivalTime(i.getstartServiceTime()-test.getloadTimeHomeCareStaff());
	}



	// Getters

	public double getTotalTravelTime() { return totalTravelTime;}
	public float getTotalServiceTime() {return totalServiceTime;}
	public double getSlack() {return slack;}
	public HashMap<Integer,Jobs> getServedJobs() {return ServedJobs;}
	public LinkedList<SubRoute> getWalkingRoutes() {return walkingRoutes;}



}
