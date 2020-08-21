import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class DrivingRoutes {

	private Inputs inp; // input problem
	private Test test; // input problem
	private Random rn;
	private  ArrayList<Route> routeList= new ArrayList<Route>();
	private  HashMap<Integer, Jobs> subJobs= new HashMap<>();
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsHighestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsMediumQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsLowestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobspatients= new ArrayList<Couple>();
	private HashMap<Integer, Jobs>assignedJobs=new HashMap<Integer, Jobs>();
	ArrayList<ArrayList<Jobs>> schift= new ArrayList<>();


	public DrivingRoutes(Inputs i, Random r, Test t, ArrayList<Couple> subJobsList) {
		inp=i;
		test=t;
		rn=r;
		this.subJobsList=subJobsList;
	}

	public void generateAfeasibleSolution() {
		// 1. Initial feasible solution
		Solution initialSol= createInitialSolution();
		// a solution is a set of routes
		// 2. VNS
		// Local search



	}

	private Solution createInitialSolution() {
		Solution initialSol= new Solution();
		creationRoutes(); // create as many routes as there are vehicles
		// iteratively insert couples - here should be a destructive and constructive method 
		ArrayList<ArrayList<Couple>> clasification= clasificationjob();
		settingStartServiceTime(); // late time
		settingAssigmentSchift(clasification);
		iteratedInsertion(clasification);
		return initialSol;
	}

	private void settingAssigmentSchift(ArrayList<ArrayList<Couple>> clasification) {
		// list of jobs
		ArrayList<Jobs> clasification3 = creationJobs(clasification.get(0));
		ArrayList<Jobs> clasification2 = creationJobs(clasification.get(1));
		ArrayList<Jobs> clasification1 = creationJobs(clasification.get(2));
		List<AttributeNurse> homeCareStaff= inp.getNurse(); // homeCareStaff 3 qualification level
		// crear arrays
		int q3= homeCareStaff.get(2).getQuantity();

		ArrayList<ArrayList<Jobs>> qualification3= assigmentHighQualification(q3,clasification3);
		//checkingWorkingTime(qualification3);
		downgradings(clasification2,qualification3);
		downgradings(clasification1,qualification3);

		//Qualification level =2
		int q2= homeCareStaff.get(1).getQuantity();
		clasification2 = creationJobs(clasification.get(1));
		clasification1 = creationJobs(clasification.get(2));
		ArrayList<ArrayList<Jobs>> qualification2= assigmentHighQualification(q2,clasification2);
		downgradings(clasification1,qualification2);

		//Qualification level =1
		int q1= homeCareStaff.get(0).getQuantity();
		clasification1 = creationJobs(clasification.get(2));
		ArrayList<ArrayList<Jobs>> qualification1= assigmentHighQualification(q1,clasification1);



		//Storing schifts
		for(ArrayList<Jobs> schifts:qualification1) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}

		for(ArrayList<Jobs> schifts:qualification2) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
		for(ArrayList<Jobs> schifts:qualification3) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
	}

	private void checkingWorkingTime(ArrayList<ArrayList<Jobs>> qualification3) {
		ArrayList<ArrayList<Jobs>> jobsListCopy= copyJobsList(qualification3);
		qualification3.clear();;
		double serviceTimeDuration=0;
		double waitingTime=0;
		double travelTimeDuration=0;
		ArrayList<Jobs> newSchift= new ArrayList<Jobs>();
		qualification3.add(newSchift);
		for(ArrayList<Jobs> list:jobsListCopy) {	
			for(int j=0; j<list.size();j++) {
				Jobs jNode=list.get(j);
				double serviceTime=jNode.getReqTime();// service time
				double waitTime=jNode.getWaitingTime();// waiting time
				double travelTime=0;// travel time
				serviceTimeDuration+=serviceTime;
				waitingTime+=waitTime;
				if(j!=list.size()-1 && jNode.getId()!=0) { // travel time
					Jobs kNode=list.get(j+1);
					travelTime=inp.getCarCost().getCost(jNode.getId(), kNode.getId());
					travelTimeDuration+=travelTime;
				}
				double duration=serviceTimeDuration+waitingTime+travelTimeDuration;
				if(duration<=296) {
				//if(duration<=test.getWorkingTime()) {
					newSchift.add(jNode);
				}
				else {
					
					newSchift= new ArrayList<Jobs>();
					newSchift.add(jNode);
					qualification3.add(newSchift);
					serviceTimeDuration=serviceTime;
					waitingTime=waitTime;
					travelTimeDuration=travelTime;
				}
			}
		}
	}

	private ArrayList<ArrayList<Jobs>> copyJobsList(ArrayList<ArrayList<Jobs>> qualification3) {
		ArrayList<ArrayList<Jobs>> jobsListCopy= new ArrayList<ArrayList<Jobs>>();
		for(ArrayList<Jobs> list:qualification3) {
			jobsListCopy.add(list);
		}
		return jobsListCopy;
	}

	private ArrayList<Jobs> creationJobs(ArrayList<Couple> qualification) {
		ArrayList<Jobs> clasification = new ArrayList<Jobs>();

		for(Couple c:qualification) {
			if(!assignedJobs.containsKey(c.getPresent().getId())) {
				clasification.add(c.getPresent());}
		}
		clasification.sort(Jobs.SORT_BY_STARTW);
		return clasification;

	}

	private void downgradings(ArrayList<Jobs> clasification2, ArrayList<ArrayList<Jobs>> qualification3) {
		for(Jobs j:clasification2) { // iterate over jobs
			for(ArrayList<Jobs> homeCare:qualification3) {
				if(!homeCare.isEmpty()) {
					boolean insertion=possibleInsertion(j,homeCare);

					if(insertion) {
						homeCare.add(j);
						assignedJobs.put(j.getId(), j);
						break;
					}
				}
			}
		}
		System.out.println("Stop");
	}

	private ArrayList<ArrayList<Jobs>> assigmentHighQualification(int q3, ArrayList<Jobs> clasification3) {
		ArrayList<ArrayList<Jobs>> qualification3= new ArrayList<> (q3);
		for(int i=0;i<q3;i++) {
			ArrayList<Jobs> schift=new ArrayList<>();
			qualification3.add(schift);
		}

		for(Jobs j:clasification3) { // iterate over jobs
			if(!assignedJobs.containsKey(j.getId())) {
				for(ArrayList<Jobs> homeCare:qualification3) {
					boolean insertion=possibleInsertion(j,homeCare);
					if(insertion) {
						assignedJobs.put(j.getId(), j);
						homeCare.add(j);
						break;
					}

				}
			}}
		System.out.println("Stop");
		return qualification3;
	}

	private boolean possibleInsertion(Jobs j, ArrayList<Jobs> homeCare) {
		boolean inserted=false;
		if(homeCare.isEmpty()) {
			inserted=true;
		}
		else {
			inserted=iterateOverSchift(j,homeCare);
		}
		return inserted;
	}

	private boolean iterateOverSchift(Jobs j, ArrayList<Jobs> homeCare) {
		boolean inserted=false;
		for(int i=0;i<homeCare.size();i++) { 
			Jobs inRoute=homeCare.get(i);
			if(i==homeCare.size()-1 ) {
				inserted=insertionLater(inRoute,j);//(inRoute)******(j)
				if(!inserted) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
				}

			}
			else {// // (inRoute)*******(j)******(inRouteK)
				if(i==0) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
				}
				if(!inserted) {
					Jobs inRouteK=homeCare.get(i+1);
					inserted=insertionIntermediateJb(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
				}
			}
		}
		return inserted;
	}

	private boolean insertionIntermediateJb(Jobs inRoute, Jobs j, Jobs inRouteK) {
		boolean inserted=insertedMiddle(inRoute,j,inRouteK);
		return inserted;
	}

	private boolean insertedMiddle(Jobs inRoute, Jobs j, Jobs inRouteK) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRoute.getId()-1);
		double possibleArrivalTime=inRoute.getstartServiceTime()-(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
		if(possibleArrivalTime<inRouteK.getstartServiceTime()) {
			if(possibleArrivalTime<=j.getstartServiceTime()) {
				inserted=true;
			}
			else {
				if(possibleArrivalTime<=j.getEndTime()) {
					//if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>=j.getStartTime()) {
					settingTimes(possibleArrivalTime,j);
					inserted=true;
				}
			}
		}

		return inserted;
	}

	private boolean insertionEarly(Jobs inRoute, Jobs j) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRoute.getId()-1);
		double possibleArrivalTime=inRoute.getstartServiceTime()-(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
		if(possibleArrivalTime<=j.getEndTime()) {
			//if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>=j.getStartTime()) {
			settingTimes(possibleArrivalTime,j);
			inserted=true;

		}
		return inserted;
	}

	private void settingTimes(double possibleArrivalTime, Jobs j) {
		j.setarrivalTime(possibleArrivalTime);
		double serviceTime=Math.max(possibleArrivalTime, j.getStartTime());
		j.setStartServiceTime(serviceTime);
		j.setWaitingTime(possibleArrivalTime, j.getstartServiceTime());	
	}

	private boolean insertionLater(Jobs inRoute,Jobs j) {//(inRoute)******(j)
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1);
		double possibleArrivalTime=inRoute.getstartServiceTime()+inRoute.getReqTime()+tv+test.getloadTimeHomeCareStaff();
		if(possibleArrivalTime<=j.getstartServiceTime()) {
			inserted=true;
			j.setWaitingTime(possibleArrivalTime, j.getstartServiceTime());
		}
		else{
			if(possibleArrivalTime<=j.getEndTime()) {
				//if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>=j.getStartTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;

			}
		}
		return inserted;
	}

	private void settingStartServiceTime() {
		for(Couple couple:subJobsList) {
			double serviceTimePresent=couple.getPresent().getEndTime();
			double serviceTimeFuture=couple.getFuture().getEndTime();
			if(serviceTimePresent==0 ||serviceTimeFuture==0 ) {
				System.out.println("Stop");
			}
			couple.getPresent().setStartServiceTime(serviceTimePresent);
			couple.getFuture().setStartServiceTime(serviceTimeFuture);
		}

	}

	private void iteratedInsertion(ArrayList<ArrayList<Couple>> clasification) {
		for(int i=0;i<clasification.size()-1;i++) {
			jobReq3Qualification(clasification.get(i)); // job with a qualification equal to 3
		}

		System.out.println("Stop");
	}

	private void jobReq3Qualification(ArrayList<Couple>qualification) {
		ArrayList<Jobs> seq1= new ArrayList<Jobs>();
		for(Couple c:qualification) {
			seq1.add(c.getPresent());
		}
		seq1.sort(Jobs.SORT_BY_STARTW);


		for(Route r:routeList) {
			for(Jobs j:seq1) {
				if(r.getDurationRoute()<test.getWorkingTime()) {
					checkFeasibleInsertion(r,j);		
				}
				else {
					routeList.add(r);
				}
			}
		}
		System.out.println("Stop");
	}

	private void checkFeasibleInsertion(Route r, Jobs toInsert) {

		if(r.getSubJobsList().isEmpty()) {
			toInsert.setarrivalTime(toInsert.getstartServiceTime());
			r.getSubJobsList().add(toInsert);
		}
		else{
			//for(int n=0;n<r.getSubJobsList().size();n++) {
			//if(n==0 || n==r.getSubJobsList().size()-1) {
			Jobs j=r.getSubJobsList().getLast(); // append at the end
			checkPossibleInsertion(j,toInsert,r);
			//}

			//}
		}

	}

	private boolean checkPossibleInsertion(Jobs j, Jobs toInsert, Route r) {
		// 1. to insert could be the job after j
		boolean insertion =false;
		if(toInsert.getStartTime()>j.getstartServiceTime()) {
			if(toInsertIsReacheable(j,toInsert)) {
				r.getSubJobsList().add(toInsert);
			}
			else {

				if(toInsert.getStartTime()<j.getstartServiceTime()) {// toInsert is early
					if(toInsertIsEARLYReacheable(j,toInsert)) {
						r.getSubJobsList().add(toInsert);
					}	
				}
			}

		}
		return insertion;
	}



	private boolean toInsertIsEARLYReacheable(Jobs j, Jobs toInsert) {
		boolean reacheble=false;
		double tv=inp.getCarCost().getCost(j.getId()-1, toInsert.getId()-1);
		double possibleArrival=toInsert.getstartServiceTime()+toInsert.getReqTime()+tv+test.getloadTimeHomeCareStaff();
		if(possibleArrival<=j.getstartServiceTime()) {
			reacheble=true;
		}
		else {
			possibleArrival=j.getstartServiceTime()-(toInsert.getReqTime()+tv+test.getloadTimeHomeCareStaff());	
			if(possibleArrival>=toInsert.getStartTime() && possibleArrival<=toInsert.getEndTime()) {
				toInsert.setarrivalTime(possibleArrival);
				toInsert.setStartServiceTime(possibleArrival);
				toInsert.setWaitingTime(possibleArrival, toInsert.getstartServiceTime());
				reacheble=true;
			}
		}
		return reacheble;
	}

	private boolean toInsertIsReacheable(Jobs j, Jobs toInsert) {
		boolean reacheable=false;
		double tv=inp.getCarCost().getCost(j.getId()-1, toInsert.getId()-1);
		double possibleArrival=j.getstartServiceTime()+j.getReqTime()+tv+test.getloadTimeHomeCareStaff();
		if(possibleArrival<toInsert.getstartServiceTime()) { // checking expected start service time
			toInsert.setarrivalTime(possibleArrival);
			toInsert.setWaitingTime(possibleArrival, toInsert.getstartServiceTime());
			reacheable=true;
		}
		else { // checking expected within TW
			if(possibleArrival>=toInsert.getStartTime() && possibleArrival>=toInsert.getEndTime()) {
				toInsert.setarrivalTime(possibleArrival);
				toInsert.setStartServiceTime(possibleArrival);
			}
		}
		return reacheable;
	}



	private boolean checkReaminingRoute(Route newRoute) {
		// 1. Time windows

		// 2. Driving working Route

		//3. Vehicle capacity

		//4. Max. detour duration

		return false;
	}


	private void creationRoutes() {
		for(int i=0;i<inp.getVehicles().size();i++) {
			int idVehicle=inp.getVehicles().get(i).getId();
			Route r= new Route();
			r.setIdRoute(idVehicle);
			routeList.add(r);
		}
	}

	private  ArrayList<ArrayList<Couple>> clasificationjob() {
		ArrayList<ArrayList<Couple>> clasification= new ArrayList<ArrayList<Couple>>();
		// 0.classified couples according the req qualification
		int i=-1;
		for(Couple c:subJobsList) {
			i++;
			subJobs.put(i, c.getPresent());

			i++;
			subJobs.put(i, c.getFuture());
			if(c.getPresent().getReqQualification()!=c.getFuture().getReqQualification()) {
				System.out.print("Error");	
			}
		}
		
		for(int qualification=0;qualification<=inp.getMaxQualificationLevel();qualification++) {
			for(Couple c:subJobsList) {
				if(c.getQualification()==qualification && qualification==0) {
					subJobspatients.add(c);
				}
				if(c.getQualification()==qualification && qualification==1) {
					subJobsLowestQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==2) {
					this.subJobsMediumQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==3) {
					this.subJobsHighestQualification.add(c);
				}
			}
		}
		clasification.add(subJobsHighestQualification);
		clasification.add(subJobsMediumQualification);
		clasification.add(subJobsLowestQualification);
		clasification.add(subJobspatients);

		return clasification;
	}

	public void assigmentHomeCareStaff() {
		ArrayList<Jobs> listserviceJobs=new ArrayList<Jobs>();
		for(Couple c:subJobsList) {
			if(c.getPresent().isClient()) {
				listserviceJobs.add(c.getPresent());
			}
			//			else {
			//				if(c.getPresent().isPatient()) {
			//					if() {
			//						
			//					}
			//				}
			//			}	
		}
		// definición de lista de trabajos por enfermenra
		List<AttributeNurse> nursesInformation=inp.getNurse();
		//		int amount=nursesInformation.get(3);
		//		ArrayList<ArrayList<Jobs>> homeCareStaff=new ArrayList<nursesInformation.>();
		//		




	}









}
