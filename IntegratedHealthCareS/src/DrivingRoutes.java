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
	private  HashMap<Integer, Jobs> assignedSubJobs= new HashMap<>();
	private  HashMap<Integer, Couple> assignedCouples= new HashMap<>();
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsHighestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsMediumQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsLowestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobspatients= new ArrayList<Couple>();
	private HashMap<Integer, Jobs>assignedJobs=new HashMap<Integer, Jobs>();
	HashMap<Integer, Jobs>downgradingsTo1=new HashMap<Integer, Jobs>();
	HashMap<Integer, Jobs>downgradingsTo3=new HashMap<Integer, Jobs>();
	
	
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
		//downgradings(downgradingsTo1,clasification1,qualification1);

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


	private void checkInsertion(Jobs job, Route r) {
		r.getSubJobsList().add(job);
		Jobs origin=job;
		Jobs end=job.getsubJobPair();
		assignedSubJobs.put(origin.getId(), origin);
		// set arrival time
		origin.setarrivalTime(origin.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		double possibleArrivalTimeFutureJob=origin.getstartServiceTime()+origin.getReqTime()+test.getloadTimeHomeCareStaff();// arrival time <- considera el load time
		r.getSubJobsList().add(end);
		assignedSubJobs.put(end.getId(), end);
		int directConnection=inp.getCarCost().getCost(origin.getId()-1, end.getId()-1);
		System.out.println("direct Connection"+directConnection);
	}

	private boolean isPickUpJob(Jobs job) {
		boolean pickUp=false;
		if(job.getTotalPeople()<0) {
			pickUp=true;
		}
		return pickUp;
	}

	private void insertionCoupleTest(Couple c, Route r) {
		destruccionProcedure(c,r);
		//construccionProcedure(c,r);
	}

	private void construccionProcedure(Couple c, Route r) {
		// para cada trabajo mirar si se puede insertar antes o despues de cada trabajo	
	}

	private void destruccionProcedure(Couple c, Route r) {
		Jobs present=c.getPresent();
		Jobs future=c.getFuture();
		if(r.getSubJobsList().isEmpty()) {
			r.addCouple(c);
		}
		else {

			//if(enoughVehicleCapacity(r,c)) {
			checkingJobByJob(r,c);
			//}

		}
	}



	private void checkingJobByJob(Route r, Couple c) {
		Route newRoute= copyRoute(r);
		int i=-1;
		for(Jobs j:r.getSubJobsList()) { // se recorre la lista de trabajos y se intenta insertar el trabajo presente
			i++;
			boolean earlyInsertion=isBeforeJ(j,c);
			if(earlyInsertion) {
				double arrivalTimePresentJob=computePossiblearrivalTime(c.getPresent(),j);
				if(arrivalTimePresentJob<=j.getstartServiceTime()) {
					c.getPresent().setarrivalTime(arrivalTimePresentJob);
					newRoute.getSubJobsList().add(i,c.getPresent());
					updateRouteCost(newRoute); // update Travel time, waiting time and service time
					boolean isFutureJobInserted= insertingFutureJob(newRoute,c);
					if(!isFutureJobInserted) {
						earlyInsertion=false;
					}
					else {
						// insert the list of jobs
						r=replaceRoute(r,newRoute); // the couples in the route are no eliminated
						// insert the couple
						r.getJobsList().add(c);		
						// check the information of the couple vs the information of the job
						boolean correctInf= checkInfSolution(r);
						System.out.println("\nCorrect information__"+correctInf);
					}
				}
			}
			if(!earlyInsertion && i+1<r.getSubJobsList().size()) {
				if(isAfterJ(j,c,r.getSubJobsList().get(i+1))) {
					double arrivalTimePresentJob=computePossiblearrivalTimeAfter(c.getPresent(),j);
					if(arrivalTimePresentJob<=c.getPresent().getstartServiceTime()) {
						c.getPresent().setarrivalTime(arrivalTimePresentJob);
						if(i+1<r.getSubJobsList().size()) {
							newRoute.getSubJobsList().add(i+1,c.getPresent());}
						else {
							newRoute.getSubJobsList().add(c.getPresent());}
						updateRouteCost(newRoute); // update Travel time, waiting time and service time
					}
				}
			}
		}

	}

	private Route copyRoute(Route r) {
		Route newRoute= new Route();
		for(Couple c:r.getJobsList()) {
			newRoute.addCouple(c);
		}
		return newRoute;
	}

	private Route replaceRoute(Route toClean, Route newRoute) {
		toClean.getSubJobsList().clear();
		for(Jobs j:newRoute.getSubJobsList()) {
			toClean.getSubJobsList().add(j);
		}
		updateRouteCost(toClean);
		return toClean;
	}

	private void updateRouteCost(Route newRoute) {
		double durationRoute=0;
		double travelTime=0; // update Travel time
		double serviceTime=0; //Start with the first service. service time
		// TO DO. waitingTime+=first job; // falta determinar la información del primer nodo en cada ruta
		double waitingTime=newRoute.getSubJobsList().getFirst().getWaitingTime(); //waiting time 
		for(int i=0;i<newRoute.getSubJobsList().size()-1;i++) {
			// se calculan los datos de la ruta tomando como referencia el nodo i 
			// pero se establece la infomación para el nodo i+1
			Jobs iJob=newRoute.getSubJobsList().get(i);
			Jobs jJob=newRoute.getSubJobsList().get(i+1);
			double doubleDirectConnection= inp.getCarCost().getCost(iJob.getId(), jJob.getId());
			double loadTimeJobJ=0;
			if(jJob.isClient()) {
				loadTimeJobJ=test.getloadTimeHomeCareStaff();
			}
			else {
				loadTimeJobJ=test.getloadTimePatient();
			}
			double serviceTimeHJobI=newRoute.getSubJobsList().get(i).getReqTime();
			double arrivalTimeJobj=iJob.getstartServiceTime()+serviceTimeHJobI+doubleDirectConnection+loadTimeJobJ;
			jJob.setarrivalTime(arrivalTimeJobj);
			travelTime+=doubleDirectConnection; //travel time
			serviceTime+=serviceTimeHJobI; //Start with the first service. service time
			waitingTime+=jJob.getWaitingTime();  // update Travel time


		}
		serviceTime+=newRoute.getSubJobsList().getLast().getReqTime();
		newRoute.setServiceTime(serviceTime);
		newRoute.setWaitingTime(waitingTime);
		newRoute.setTravelTime(travelTime);
		durationRoute=travelTime+serviceTime+waitingTime;
		newRoute.setDurationRoute(durationRoute);
	}

	private double computePossiblearrivalTimeAfter(Jobs toInsert, Jobs inRoute) {
		double arrivalTime=0;
		double loadUnloadTime=0;
		if(toInsert.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
		}
		double travelTime= inp.getCarCost().getCost(inRoute.getId(),toInsert.getId());
		int previousUser=0;
		if(inRoute.isClient()) {
			previousUser=test.getloadTimeHomeCareStaff();
		}
		else {
			previousUser=test.getloadTimePatient();
		}	
		double timeAtPreviousNode=inRoute.getstartServiceTime()+inRoute.getReqTime()+previousUser;
		double previousTime=timeAtPreviousNode+travelTime+loadUnloadTime;
		arrivalTime=inRoute.getstartServiceTime()+previousTime;
		return arrivalTime;
	}

	private boolean checkInfSolution(Route r) {
		boolean correctInformation=false;
		int i=-1;
		for(Couple c:r.getJobsList()) {
			i++;
			if(c.getPresent()==r.getSubJobsList().get(i)) {
				i++;
				if(c.getFuture()==r.getSubJobsList().get(i)) {
					correctInformation=true;
					System.out.println("\nCouples Information" + c.toString());
					System.out.println("\nJob Information");
					System.out.println("\nJob Present" + c.getPresent());
					System.out.println("\nJob Future" + c.getFuture());
				}	
			}

		}
		return correctInformation;
	}




	private boolean insertingFutureJob(Route newRoute, Couple c) {
		boolean inserted= false;
		int sizeRoute=newRoute.getSubJobsList().size();
		for(int i=sizeRoute-1;i>=0;i--) { // checking backward Route
			Jobs jobI=newRoute.getSubJobsList().get(i);	
			if(lastJobFeasible(c,jobI)) { // last job
				newRoute.getSubJobsList().add(c.getFuture());
				inserted=checkReaminingRoute(newRoute); // the solution is repaired 
			}
		}
		return inserted;
	}

	private boolean checkReaminingRoute(Route newRoute) {
		// 1. Time windows

		// 2. Driving working Route

		//3. Vehicle capacity

		//4. Max. detour duration

		return false;
	}

	private boolean lastJobFeasible(Couple c, Jobs jobI) {
		boolean insertion=false;
		double directConnection = inp.getCarCost().getCost(jobI.getId(), c.getFuture().getId());
		double loadTime=0;
		if(jobI.isClient()) {
			loadTime=test.getloadTimeHomeCareStaff();
		}
		double possibelArrivalTime=jobI.getstartServiceTime()+jobI.getReqTime()+loadTime+directConnection;
		if(possibelArrivalTime>=c.getFuture().getStartTime() && possibelArrivalTime<=c.getFuture().getEndTime()) { // TW
			if(possibelArrivalTime-c.getPresent().getArrivalTime()<=test.getDetour()) { // Max detour duration
				c.getFuture().setarrivalTime(possibelArrivalTime);
				insertion=true;	
			}
		}
		return insertion;
	}

	private double computePossiblearrivalTime(Jobs toInsert, Jobs inRoute) {
		double arrivalTime=0;
		double loadUnloadTime=0;
		if(toInsert.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
		}
		double tarvelTime= inp.getCarCost().getCost(toInsert.getId(), inRoute.getId());
		double previousTime=loadUnloadTime+tarvelTime;
		arrivalTime=toInsert.getstartServiceTime()+previousTime;
		return arrivalTime;
	}


	private boolean isAfterJ(Jobs j, Couple c, Jobs l) {
		boolean lateJob=false;
		//if(c.getPresent().getstartServiceTime()>j.getstartServiceTime() && c.getPresent().getstartServiceTime()<l.getstartServiceTime()) {
		if(c.getPresent().getstartServiceTime()>j.getstartServiceTime() ) {

			lateJob=true;
		}
		return lateJob;

	}

	private boolean isBeforeJ(Jobs j, Couple c) {
		boolean earlyJob=false;
		System.out.print("\nJob to insert "+c.getPresent().getstartServiceTime());
		System.out.print("\nJob in route "+j.getstartServiceTime());
		if(c.getPresent().getstartServiceTime()<j.getstartServiceTime()) {
			earlyJob=true;
		}
		return earlyJob;
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
