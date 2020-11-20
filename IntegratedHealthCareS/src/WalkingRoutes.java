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
	private double durationWalkingRoute=0;
	private double totalTravelTime = 0; // route total travel time
	private double totalServiceTime = 0;// route total service time in the route
	private double waitingTime=0.0F; // Waiting time
	private double slack=0;
	private LinkedList<SubRoute> jobSlots; // list of subroutes which describe the job sequence respecting all restrictions
	private ArrayList<Jobs> jobList; // es la lista que almacena todos los trabajos
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
		for(SubRoute wr:jobSlots) {
			if(wr.getJobSequence().size()>1) {
				System.out.println("Duration  "+wr.getDurationWalkingRoute());  // to remove
//			System.out.print("\n WR_Cost_ iD  "+wr.getSlotID()+" "+ wr.getTotalTravelTime());  // to remove
//			for(Jobs j:wr.getJobSequence()) {
//				System.out.print(" j_( Id" + j.getId()+", B_"+j.getstartServiceTime()+") "); // to remove
//			}
//			System.out.print("\n");
			}
		}
		slackprocedureWalkingRoute(jobSlots);
		computingRouteTimeDuration(jobSlots); // computing the travel time for each slot walking time + waiting time
		settingCostIndividualRoutes();
		completingIndividualRoutes();

		// 5. To Do: Improvement for slots   Method to improve the slots
		//??? PERT!!

		// 6.1 Computing walking cost



		// 6.2 To Do: Computing walking time duration: Method to determine the start time for each job in the walking route - apply PERT method for each slot

	
		
		//computingWalkingTimeDuration(jobSlots); // computing the travel time for each slot walking time + waiting time

		System.out.println("potential WR");
		for(SubRoute wr:jobSlots) {
			if(wr.getJobSequence().size()>1) {
				System.out.println("Duration  "+wr.getDurationWalkingRoute());  // to remove
//			System.out.print("\n WR_Cost_ iD  "+wr.getSlotID()+" "+ wr.getTotalTravelTime());  // to remove
//			for(Jobs j:wr.getJobSequence()) {
//				System.out.print(" j_( Id" + j.getId()+", B_"+j.getstartServiceTime()+") "); // to remove
//			}
//			System.out.print("\n");
			}
		}
		// 7. Solving set partitioning problem
		ExactAllocation improveSlots= new ExactAllocation(test,inp);
		improveSlots.selectionWalkingRoutes(jobSlots,walkingRoutes);
		walkingRoutes=improveSlots.getWalkingRoutes();


		System.out.print("\n Final WR");

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
		LinkedList<SubRoute> copy= new LinkedList<SubRoute> ();
		for(SubRoute s:walkingRoutes) {
			if(s.getJobSequence().size()>1) {
				copy.add(s);
			}

		}
		walkingRoutes.clear();
		int walkRouteID=-1;
		for(SubRoute s:copy) {
			walkRouteID++;
			s.setSlotID(walkRouteID);
				walkingRoutes.add(s);
			}
		// checkSolutionQuality <- each job is once in the set of solutions
		boolean goodSolution = checkWalkingRouteSet();
		System.out.print("\n good Solution" + goodSolution);
		
		//slackprocedureWalkingRoute();
		computingRouteTimeDuration(walkingRoutes); // computing the travel time for each slot walking time + waiting time
	
		// 8. Making walking routes into big tasks 
		walkingRouteToJob(); // fix the pick-up and drop-off nodes for each walking route
	}


	//LinkedList<SubRoute> walkingRoutes
	private void settingTimes(LinkedList<SubRoute> walk) {
		for(SubRoute s:walk) {
			for(Jobs j:s.getJobSequence()) {
		double departureTime=j.getstartServiceTime()+j.getReqTime();
		j.setdepartureTime(departureTime);
		j.setEndServiceTime(departureTime);
			}
		}
	}


	//LinkedList<SubRoute> walkingRoutes
	private void slackprocedureWalkingRoute(LinkedList<SubRoute> walk) {
		settingTimes(walk);
		for(SubRoute s:walk) {
			int length=	s.getJobSequence().size()-1;
			for(int i=length;i>0;i--) {// iterating over route
				
				Jobs j=s.getJobSequence().get(i-1); // los tiempos de este nodo cambian
				Jobs k=s.getJobSequence().get(i); // este es el nodo de referencia 
				double departure=k.getDepartureTime();
				double startServiceTime=departure-k.getReqTime();
				double arrival=startServiceTime;
				if(i==length) { // se modifican los tiempos del ultimo nodo
					arrival=Math.max(startServiceTime, k.getStartTime());
					k.setWaitingTime(arrival, startServiceTime);
					k.setarrivalTime(arrival);
					k.setdepartureTime(departure+test.getloadTimeHomeCareStaff());
				}
				double travelTime=inp.getWalkCost().getCost(j.getId()-1, k.getId()-1);
				 departure=k.getArrivalTime()-travelTime;
				 startServiceTime=departure-j.getReqTime();
				 arrival=startServiceTime;
				if(startServiceTime>=j.getStartTime() && startServiceTime<=j.getEndTime()) {
					arrival=Math.max(startServiceTime, j.getStartTime());
					j.setarrivalTime(arrival);
					j.setStartServiceTime(startServiceTime);
					j.setEndServiceTime(departure);
					j.setdepartureTime(departure);
					j.setWaitingTime(arrival, startServiceTime);
				}
				
				if(i==1) {// para el inicio de la ruta considerar el cargue y el descargue 
					j.setWaitingTime(arrival+test.getloadTimeHomeCareStaff(), startServiceTime);
					j.setarrivalTime(arrival-test.getloadTimeHomeCareStaff());
				}
			}
			System.out.println("route");
			System.out.println(s.toString());
			System.out.println("route");
		}	
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

	//inkedList<SubRoute> walkingRoutes
	private void computingRouteTimeDuration(LinkedList<SubRoute> walk) {

		double service=0;
		double waitingTime=0;
		double travelTime=0;
		// service time and waiting time
		for(SubRoute wr:walk) {
			computeRouteDurtion(wr);
	}

		// total values
		service=0;
		waitingTime=0;
		travelTime=0;
		double durationRoute=0;
		for(SubRoute wr:walk) {
			double serviceRoute=0;
			double waitingRoute=0;
			double travelRoute=0;
			double durationR=0;
			if(wr.getJobSequence().size()>1) { // route has more than one job
				serviceRoute=wr.getTotalServiceTime();
				service+=serviceRoute;
				waitingRoute=wr.getDurationWaitingTime();
				waitingTime+=waitingRoute;
				travelRoute=wr.getTotalTravelTime();
				travelTime+=travelRoute;
				durationR=wr.getDurationWalkingRoute();
				durationRoute+=durationR;
			}
		}

		this.totalServiceTime=service;
		this.durationWalkingRoute=durationRoute;
		this.totalTravelTime=travelTime;
		this.waitingTime=waitingTime;
	}


	//	private double totalTravelTime = 0.0; // route total travel time
	//	private float totalServiceTime = 0.0F; // route total service time in the route
	//	private float waitingTime=0.0F; // Waiting time



	private void completingIndividualRoutes() {
		for(Jobs j:this.inp.getclients().values()) {
			this.ServedJobs.put(j.getId(),j);	
		}
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
			System.out.println(wr.toString());
			if(wr.getJobSequence().size()>1) {// only for real walking routes
				wr.setDropOffNode(wr.getJobSequence().getFirst());

				wr.setPickUpNode(wr.getJobSequence().getLast());

				/*
				 *  Present job
				 */
				Jobs presentbigJob=new Jobs(wr.getDropOffNode());
				// 1. Setting the TW and start service time
				double startTime=wr.getDropOffNode().getstartServiceTime(); // considering the unloading of the home health care staff
				presentbigJob.setStartTime(startTime);
				presentbigJob.setEndTime(startTime);
				presentbigJob.setStartServiceTime(startTime);
				// 2. Setting the duration of the jobs
				presentbigJob.setserviceTime(0); 
				// 3. Setting qualification of the nurse
				presentbigJob.setReqQualification(wr.getSkill());

				/*
				 *  Future job
				 */
				Jobs futurebigJob=new Jobs(wr.getPickUpNode());		
				// 1. Setting the TW and start service time
				futurebigJob.setStartTime(wr.getPickUpNode().getstartServiceTime()+ wr.getDurationWalkingRoute());
				futurebigJob.setEndTime(wr.getPickUpNode().getstartServiceTime()+ wr.getDurationWalkingRoute());
				// 2. Setting the duration of the jobs
				futurebigJob.setserviceTime(wr.getDurationWalkingRoute()); 
				// 3. Setting qualification of the nurse
				SubJobs separateJobs= new SubJobs(presentbigJob,futurebigJob,wr);
				this.subJobList.add(separateJobs);
			}
		}
	}



	private void jobsInsertion(String serviceStartTime) {
		for(Jobs job:jobList) {
			for(SubRoute wr:jobSlots) {
				computeRouteDurtion(wr);
				if(wr.getSlotID()==9) {
					System.out.println("\nRoute\n");
				}
				Jobs i=new Jobs(job);
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
				if(wr.getSlotID()==9) {
					System.out.println("\nRoute\n");
				}
				//i.setWaitingTime(i.getstartServiceTime(), i.getArrivalTime());
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
		System.out.print("Walking duration ");
		
		System.out.println("Walking duration "+ wr.getDurationWalkingRoute());	
		if(wr.getDurationWalkingRoute()>=test.getWorkingTime()) {
			System.out.print("Walking duration ");	
		}
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
			if((wr.getDurationWalkingRoute()+newJobTime)<test.getWorkingTime()) { // if the new job doesn´t exceed the maximum working hours
				reacheable=true; // job i will be inserted in the Route wr
				i.setarrivalTime(possibleArrivalTime);
				double serviceStartTime=Math.max(i.getArrivalTime(), i.getStartTime());
				i.setStartServiceTime(serviceStartTime);
			}
		}
		return reacheable;
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
		//i.setarrivalTime(i.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		i.setarrivalTime(i.getstartServiceTime()); // no se considera tiempo de descargue
	}



	// Getters


	public double getTotalwaitingTime() { return waitingTime;}
	public double getTotalTravelTime() { return totalTravelTime;}
	public double getTotalServiceTime() {return totalServiceTime;}
	public double getSlack() {return slack;}
	public HashMap<Integer,Jobs> getServedJobs() {return ServedJobs;}
	public LinkedList<SubRoute> getWalkingRoutes() {return walkingRoutes;}
	public double getdurationWalkingRoute() { return durationWalkingRoute;}


}
