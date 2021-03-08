import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import umontreal.iro.lecuyer.rng.LFSR113;
import umontreal.iro.lecuyer.rng.RandomStreamBase;

public class DrivingRoutes {

	private Inputs inp; // input problem
	private Test test; // input problem
	private Random rn;
	// Routes


	private  HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff <id drop off node, couple>
	private  HashMap<String, Couple> dropoffpatientMedicalCentre= new HashMap<>();// hard time windows list of patient  <id drop off node, couple>
	private  HashMap<String, Couple> pickpatientMedicalCentre= new HashMap<>();// soft time windows list of patient    <id pick up node, couple>
	private  HashMap<String, Couple> pickUpHomeCareStaff= new HashMap<>();// soft time windows list of home care staff <id pick up node, couple>
	private  double[][] coverageMatrix;
	private  double[][] timeMatrix;


	// computing walking hours
	private LinkedList<SubRoute> walkingRoutes;
	private  HashMap<Integer, SubRoute> jobsInWalkingRoute= new HashMap<>();
	///
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsHighestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsMediumQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsLowestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobspatients= new ArrayList<Couple>();
	private HashMap<String, SubJobs>assignedJobs=new HashMap<String, SubJobs>();
	private ArrayList<Parts> schift= new ArrayList<>();


	// list of assigned jobs to vehícules _ option 1
	ArrayList<Parts> sequenceVehicles= new ArrayList<>(); // turn for paramedics


	// list of assigned jobs qualification paramedics
	ArrayList<Parts> qualificationParamedic= new ArrayList<>(); // turn for paramedics
	ArrayList<ArrayList<SubJobs>> qualification1= new ArrayList<>();  // turn for home care staff 1
	ArrayList<ArrayList<SubJobs>> qualification2= new ArrayList<>();  // turn for home care staff 2
	ArrayList<ArrayList<SubJobs>> qualification3= new ArrayList<>();  // turn for home care staff 2
	// subJobs couple
	HashMap<String, Couple> coupleList= new HashMap<>();  // turn for home care staff 2
	private  HashMap<String, SubJobs> originConnectionlist= new HashMap<>(); // it returns the present subjob associated to a future node
	private  HashMap<String, SubJobs> destinationConnectionlist= new HashMap<>(); // it returns the future subjob associated to a present node

	private Solution initialSol=null;
	private Solution solution=null;
	// to remove
	private HashMap<String,SubJobs> assignedSubJobs= new HashMap<String,SubJobs>();// Security copy
	private HashMap<String,SubJobs> missingubJobs= new HashMap<String,SubJobs>();// Security copy


	// frequency
	int[][] frequency;

	public DrivingRoutes(Inputs i, Random r, Test t, HashMap<String, Couple> subJobsList2,  LinkedList<SubRoute> subroutes) {
		inp=i;
		test=t;
		rn=r;
		walkingRoutes=subroutes;
		timeMatrix=new double[inp.getNodes().size()][4];
		coverageMatrix=new double[inp.getNodes().size()][inp.getNodes().size()];
		this.coupleList=subJobsList2;
		if(subroutes!=null) {
			for(SubRoute wr:subroutes) {
				Jobs startNode=wr.getJobSequence().get(0);
				jobsInWalkingRoute.put(startNode.getId(), wr);
			}
		}
	}

	public void generateAfeasibleSolution( int iteration, Solution sol, boolean diversification) { 
		Solution newSol=null;
		if(diversification || sol==null) {
			initialSol= createInitialSolution(iteration); // la ruta ya deberia tener los arrival times
			//System.out.println(initialSol.toString());
			newSol= new Solution (initialSol);
		}
		else {
			newSol= new Solution (sol);
		}
		solution= assigningRoutesToDrivers(iteration,newSol);

		ArrayList<Jobs> missingJobs= checkingMissingJobs(newSol);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}
		boolean goodSolution=solutionValitadion(solution);


		if(!goodSolution) {
			//System.out.println("Stop");
		}

		//System.out.println("Stop");

	}


	private ArrayList<Jobs> checkingMissingJobs(Solution newSol) {
		ArrayList<Jobs> missingJobs = new ArrayList<>();
		HashMap<String, Jobs> missing = new HashMap<>();
		HashMap<String, Jobs> listJobs = new HashMap<>();
		for(Couple c: dropoffHomeCareStaff.values()) {
			listJobs.put(c.getPresent().getSubJobKey(), c.getPresent());
			listJobs.put(c.getFuture().getSubJobKey(), c.getFuture());
		}
		for(Couple c: dropoffpatientMedicalCentre.values()) {
			listJobs.put(c.getPresent().getSubJobKey(), c.getPresent());
			listJobs.put(c.getFuture().getSubJobKey(), c.getFuture());
		}
		for(Couple c: pickpatientMedicalCentre.values()) {
			listJobs.put(c.getPresent().getSubJobKey(), c.getPresent());
			listJobs.put(c.getFuture().getSubJobKey(), c.getFuture());
		}

		for(Jobs j: listJobs.values()) {
			if(j.getSubJobKey().equals("D17")) {
				//System.out.println("Stop");
			}
			boolean assigned=false;
			for(Route r:newSol.getRoutes()) {
				if(r.getJobsDirectory().containsKey(j.getSubJobKey())) {
					assigned=true;
					break;
				}
			}
			if(!assigned) {
				missing.put(j.getSubJobKey(), j);
			}
		}
		for(Jobs j: missing.values()) {
			missingJobs.add(j);
		}

		return missingJobs;
	}

	private boolean solutionValitadion(Solution solution) {
		System.out.println(solution.toString());
		boolean noError= false;
		ArrayList<Route> list= new ArrayList<Route> ();
		for(Route r:solution.getRoutes()) {
			list.add(r);
		}
		boolean feasible= validationSequenceRoutes(list);
		if(!feasible) {
			//System.out.println("Stop");
		}

		boolean equivalent=true;
		for(Route r:solution.getRoutes()) {
			equivalent=true;
			for(SubJobs j:r.getSubJobsList()) {
				Route r2=selectionRoute(j,solution.getShift());
				SubJobs driver=r.getJobsDirectory().get(j.getSubJobKey());
				SubJobs medicalStaff=r2.getJobsDirectory().get(j.getSubJobKey());
				equivalent= checkingDeriverRouteVSshift(driver,medicalStaff);
				if(!equivalent) {
					break;
				}
			}

			if(!equivalent) {
				break;
			}
		}

		if(feasible && equivalent) {
			noError=true;
			//System.out.println("Stop");
		}
		return noError;
	}

	private boolean checkingDeriverRouteVSshift(SubJobs driver, SubJobs medicalStaff) {
		boolean equivalent= false;

		if(driver.getArrivalTime()==medicalStaff.getArrivalTime() && driver.getDepartureTime()==medicalStaff.getDepartureTime()) { // arrival and departure
			if(driver.getstartServiceTime()==medicalStaff.getstartServiceTime() && driver.getendServiceTime()==medicalStaff.getendServiceTime()) { // start service time and end service time
				equivalent= true;
			}
		}

		return equivalent;
	}




	private Solution sortInsertionProcedure(int iteration,boolean timeOrden) {

		int amountVehicles=Integer.MAX_VALUE;
		Solution sol=null;
		HashMap<String, Couple> dropoffHomeCareStaff= selectingHomeCareStaffCouple();// hard time windows list of home care staff 
		HashMap<String, Couple> pickupHomeCareStaff= selectingHomeCareStaffPickUpCouple(dropoffHomeCareStaff);
		HashMap<String, Couple> dropoffpatientMedicalCentre= selectingCoupleDropOffMedical();// hard time windows list of patient
		HashMap<String, Couple> pickpatientMedicalCentre= selectingCouplepickpatientMedicalCentre(dropoffpatientMedicalCentre);// soft time windows list of patient
		boolean feasibleStaff=false;
		do {
			while(inp.getVehicles().get(0).getQuantity()<amountVehicles) {
				HashMap<String,SubJobs> toInsert= new HashMap<String,SubJobs>();
				ArrayList<Jobs> insertionOrder=sortingJobsList(dropoffHomeCareStaff,dropoffpatientMedicalCentre);
				// pool of routes
				ArrayList<Parts> poolParts= poolPartscreationRoute();		

				sol= solutionGenerator(iteration,insertionOrder,poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,pickupHomeCareStaff,toInsert);
				amountVehicles=sol.getRoutes().size();
			}
			ArrayList<Route> poolRoutes=new ArrayList<>();
			if(sol!=null) {
				for(Route r:sol.getRoutes()) {
					poolRoutes.add(r);
				}
			}
			//definition of shifts
			Solution shift= shiftDefinition(iteration,sol);
			shift.setShift(shift);
			sol.setShift(shift);
			sol.checkingSolution(inp, test, jobsInWalkingRoute, shift);
			Solution dummySolution = checkSolution(dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre);
			boolean goodSolution=solutionValitadion(shift);

			//feasibleStaff=true;
			feasibleStaff=checkingFeasibility(sol);
			//System.out.println(sol.toString());
		}while(!feasibleStaff);
		boolean goodSolution=solutionValitadion(sol);
		if(!goodSolution) {
			//System.out.println("Stop");
		}
		return sol;
	}


	private boolean checkingFeasibility(Solution solution) {
		boolean feasible= true;
		// en terminos del personal
		int q1=0;
		int q2=0;
		int q3=0;
		int paramedics=0;
		for(Route r:solution.getShift().getRoutes()) {
			if(r.getAmountParamedic()>0) {
				paramedics++;
			}
			else { // home care staff

				switch(r.getQualificationLevel()){
				case 1:
					q1++;
					break;
				case 2:
					q2++;
					break;
				default:
					q3++;
					break;
				}
			}
		}
		// en termino del vehículo
		int numVeh=solution.getRoutes().size();

		boolean testMedicalStaff=false;
		//System.out.println("paramedics: used " + paramedics+ "avaliability "+inp.getParamedic().get(0).getQuantity());
		//System.out.println("q1: used " + q1+ "avaliability "+inp.gethomeCareStaffInf().get(0).getQuantity());
		//System.out.println("q2: used " + q2+ "avaliability "+inp.gethomeCareStaffInf().get(1).getQuantity());
		//System.out.println("q3: used " + q2+ "avaliability "+inp.gethomeCareStaffInf().get(2).getQuantity());
		if(paramedics<= inp.getParamedic().get(0).getQuantity() && q1<= inp.gethomeCareStaffInf().get(0).getQuantity() && q2<= inp.gethomeCareStaffInf().get(1).getQuantity() && q3<= inp.gethomeCareStaffInf().get(2).getQuantity()) {
			testMedicalStaff=true;
		}
		boolean testVehicle=false;
		if(numVeh<=inp.getVehicles().get(0).getQuantity()) {
			testVehicle=true;
		}

		ArrayList<Jobs> missingJobs= checkingMissingJobs(solution);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}


		boolean waitingTime=computingWaitingTime(solution);

		if(waitingTime) {
			//System.out.println("Stop");
		}

		if(testVehicle && testMedicalStaff && missingJobs.isEmpty()) {
			feasible=true;
		}


		return feasible;
	}
	private boolean computingWaitingTime(Solution solution2) {
		boolean feasible=false;
		for(Route r:solution2.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				double additionaltime=determineAdditionalTime(j);

				double delta=(j.getstartServiceTime()-(j.getArrivalTime()+additionaltime));
				if(delta>test.getCumulativeWaitingTime()) {
					//System.out.println(j.toString());
				}
			}

		}
		return feasible;
	}

	private Solution constructionSol(ArrayList<SubJobs> insertionOrder, ArrayList<Parts> poolParts, HashMap<String, Couple> dropoffHomeCareStaff,
			HashMap<String, Couple> dropoffpatientMedicalCentre, HashMap<String, Couple> pickpatientMedicalCentre,
			HashMap<String, Couple> pickupHomeCareStaff, HashMap<String, SubJobs> toInsert) {
		HashMap<String, SubJobs> backUp = new HashMap<String, SubJobs>();
		boolean hard= true;
		boolean hardConstraints= false;
		HashMap<String, SubJobs> check = new HashMap<String, SubJobs>();
		for(SubJobs j: insertionOrder) {
			check.put(j.getSubJobKey(), j);
		}
		do {
			for(Jobs j: insertionOrder) {
				if(j.getSubJobKey().equals("D67")) {
					//System.out.println(j.toString());
				}
				if(j.getSubJobKey().equals("D5167")) {
					//System.out.println(j.toString());
				}
				if(j.getSubJobKey().equals("D4658")) {
					//System.out.println(j.toString());
				}

				boolean inserted= insertionJobSelectingRoute(poolParts,j,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,pickupHomeCareStaff,toInsert,hardConstraints);
				if(inserted) {
					insertionOrder.remove(j);
					toInsert.clear();
					//insertionOrder.sort(Jobs.TWSIZE_Early);
					int sortCriterion=rn.nextInt(2)+1;// 1 distance depot // 2 early TW // 3 size 
					switch(sortCriterion){
					case 1:  insertionOrder.sort(Jobs.SORT_BY_STARTW);
					break;
					case 2:  insertionOrder.sort(Jobs.TWSIZE_Early);
					break;

					}
					break;
				}
			}
			if(insertionOrder.isEmpty() && hard) {
				calculatingTW(poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,backUp);

				insertingSoftConstraints(backUp,poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre);
				for(SubJobs j:backUp.values()) {
					if(j.isClient()||j.isMedicalCentre()) {
						insertionOrder.add(j);}
				}
				hard=false;
				backUp.clear();


			}
			ArrayList<Jobs> missingJobs= checkingMissingJobs(poolParts);
			for(Parts p:poolParts) {
				if(p.getDirectorySubjobs().containsKey("D41")) {
					//System.out.println("Sol");
				}
				if(p.getDirectorySubjobs().containsKey("P41")) {
					//System.out.println("Sol");
				}
			}
		}while(!insertionOrder.isEmpty());

		for(Parts p:poolParts) {
			if(p.getDirectorySubjobs().containsKey("P36")) {
				//System.out.println(p.toString());
			}
			if(p.getDirectorySubjobs().containsKey("D8")) {
				//System.out.println(p.toString());
			}
		}
		//	slackMethod(poolParts);
		//mergingParts(poolParts);  // merging Parts
		ArrayList<Jobs> missingJobs= checkingMissingJobs(poolParts);
		ArrayList<Route> poolRoutes=insertingDepotConnections(poolParts);
		boolean goodSolution=validationSequenceRoutes(poolRoutes);	
		mergingRoutes(poolRoutes);

		goodSolution=validationSequenceRoutes(poolRoutes);		
		goodSolution=validationSequenceRoutes(poolRoutes);
		//slackTime(poolRoutes);
		stackingRoutes(poolRoutes);
		goodSolution=validationSequenceRoutes(poolRoutes);
		poolRoutes.sort(Route.SORT_BY_EarlyJob);

		//		boolean goodSolution=validationSequenceRoutes(poolRoutes);
		//		//System.out.println("Routes list ");
		//		for(Route route:poolRoutes) {
		//			//System.out.println("Route "+ route.toString());
		//		}

		Solution solution= new Solution ();
		for(Route route:poolRoutes) {
			if(!route.getPartsRoute().isEmpty()) {
				solution.getRoutes().add(route);}
		}
		solution.checkingSolution(inp, test, jobsInWalkingRoute, solution);
		goodSolution=validationSequenceRoutes(poolRoutes);
		//System.out.println(solution.toString());

		missingJobs= checkingMissingJobs(solution);
		//		Route r1=selectionRoute(null,solution);
		//		Route r2=selectionRoute1(null,solution);
		//solution.checkingSolution(inp, test, jobsInWalkingRoute, solution);
		return solution;
	}


	private Solution solutionGenerator(int iteration, ArrayList<Jobs> insertionOrder, ArrayList<Parts> poolParts, HashMap<String, Couple> dropoffHomeCareStaff,
			HashMap<String, Couple> dropoffpatientMedicalCentre, HashMap<String, Couple> pickpatientMedicalCentre,
			HashMap<String, Couple> pickupHomeCareStaff, HashMap<String, SubJobs> toInsert) {
		HashMap<String,SubJobs> backUp= new HashMap<>();
		boolean hardConstraints=true;
		do {

			for(Jobs j: insertionOrder) {

				if(j.getSubJobKey().equals("D4961")) {
					System.out.println("Stop");
				}
				if(j.getSubJobKey().equals("D4853 ")) {
					System.out.println("Stop");
				}
				if(j.getSubJobKey().equals("P12")) {
					//System.out.println("Stop");
				}
				if(j.getSubJobKey().equals("D68")) {
					//System.out.println("Stop");
				}
				boolean inserted= insertionJobSelectingRoute(poolParts,j,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,pickupHomeCareStaff,toInsert,hardConstraints);
				if(inserted) {
					insertionOrder.remove(j);
					if(!toInsert.isEmpty()) {
						//System.out.println("Sol");
					}
					//HashMap<String,SubJobs> backUp= new HashMap<>();
					if(hardConstraints) {
						for(SubJobs sb: toInsert.values()){
							backUp.put(sb.getSubJobKey(),(SubJobs) sb);
						} }
					toInsert.clear();
					//insertionOrder.sort(Jobs.TWSIZE_Early);
					int sortCriterion=rn.nextInt(3)+1;// 1 distance depot // 2 early TW // 3 size 
					switch(sortCriterion){
					case 1:  insertionOrder.sort(Jobs.SORT_BY_STARTW);
					break;
					case 2:  insertionOrder.sort(Jobs.TWSIZE_Early);
					break;
					case 3:  Collections.shuffle(insertionOrder,rn);
					break;
					}
					break;
				}
			}

			if(insertionOrder.isEmpty() && hardConstraints) {
				for(Parts p:poolParts) {
					if(p.getDirectorySubjobs().containsKey("D41")) {
						//System.out.println(p.getDirectorySubjobs().get("D41"));
						//System.out.println("Sol");
					}
					if(p.getDirectorySubjobs().containsKey("P41")) {
						//System.out.println("Sol");
					}
				}
				calculatingTW(poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,backUp);
				for(SubJobs j:backUp.values()) {
					if(j.isClient() || j.isMedicalCentre()) {
						insertionOrder.add(j);
					}
				}
				hardConstraints=false;
				backUp.clear();
			}
			for(Parts p:poolParts) {
				if(p.getDirectorySubjobs().containsKey("D41")) {
					//System.out.println(p.getDirectorySubjobs().get("D41"));
					//System.out.println("Sol");
				}
				if(p.getDirectorySubjobs().containsKey("P41")) {
					//System.out.println(p.getDirectorySubjobs().get("P41"));
					//System.out.println("Sol");
				}
			}

		}while(!insertionOrder.isEmpty());

		//	mergingParts(poolParts);  // merging Parts
		slackMethod(poolParts);
		ArrayList<Route> poolRoutes=insertingDepotConnections(poolParts);
		boolean goodSolution=validationSequenceRoutes(poolRoutes);
		mergingRoutes(poolRoutes);
		goodSolution=validationSequenceRoutes(poolRoutes);
		stackingRoutes(poolRoutes);
		goodSolution=validationSequenceRoutes(poolRoutes);
		poolRoutes.sort(Route.SORT_BY_EarlyJob);


		Solution solution= new Solution ();
		for(Route route:poolRoutes) {
			if(!route.getPartsRoute().isEmpty()) {
				solution.getRoutes().add(route);}
		}
		solution.setShift(solution);
		solution.checkingSolution(inp, test, jobsInWalkingRoute, solution);
		//System.out.println(solution.toString());

		ArrayList<Jobs> missingJobs= checkingMissingJobs(solution);
		if(!missingJobs.isEmpty()) {

			//System.out.println(iteration);
			//System.out.println("Stop");
		}

		Solution newSol= reducingVehicles(1,solution);
		//System.out.println("Sol");
		//System.out.println(newSol.toString());
		boolean feasibleStaff=checkingFeasibility(solution);
		goodSolution=validationSequenceRoutes(poolRoutes);
		//System.out.println(solution.toString());
		return solution;
	}

	private void slackMethod(ArrayList<Parts> poolRoutes) {
		double waitingTime=0;
		for(Parts r:poolRoutes) {
			waitingTime= computeW(r);
			//	if(waitingTime>0) {
			double departure=0;
			double startServiceTimePossible=0;
			double arrival=r.getListSubJobs().get(r.getListSubJobs().size()-1).getArrivalTime();
			double loadDownloadTime=computingloadDownload(r.getListSubJobs().get(r.getListSubJobs().size()-1),test);
			double registration=computingTimeBefore(r.getListSubJobs().get(r.getListSubJobs().size()-1),test);

			if(r.getListSubJobs().get(r.getListSubJobs().size()-1).getTotalPeople()<0) {
				departure=r.getListSubJobs().get(r.getListSubJobs().size()-1).getstartServiceTime()-registration;
				arrival=r.getListSubJobs().get(r.getListSubJobs().size()-1).getstartServiceTime()-registration-loadDownloadTime;
			}
			else {
				departure=r.getListSubJobs().get(r.getListSubJobs().size()-1).getstartServiceTime()+loadDownloadTime;
				arrival=r.getListSubJobs().get(r.getListSubJobs().size()-1).getstartServiceTime();
			}

			double arrivalLast=arrival;
			double departureLast=departure;


			for(int i=r.getListSubJobs().size()-2;i>=0;i--) {
				SubJobs iJobs=r.getListSubJobs().get(i); // se calculan los tiempos para este nodo
				SubJobs jJobs=r.getListSubJobs().get(i+1);

				if(iJobs.getSubJobKey().equals("D4961")) {
					System.out.println("Stop");
				}

				if(iJobs.getSubJobKey().equals("D4972")) {
					System.out.println("Stop");
				}

				double tv= inp.getCarCost().getCost(iJobs.getId()-1, jJobs.getId()-1);
				departure=arrival-tv;
				startServiceTimePossible=0;
				loadDownloadTime=computingloadDownload(iJobs,test);
				registration=computingTimeBefore(iJobs,test);
				if(iJobs.getTotalPeople()<0) {
					startServiceTimePossible=departure-registration;
				}
				else {
					startServiceTimePossible=departure-loadDownloadTime-registration;
				}
				boolean timeWindow=computingFeasibility(startServiceTimePossible,departure,iJobs);
				if(timeWindow) {
					startServiceTimePossible=computingServiceStartTime(startServiceTimePossible,iJobs);
					if(iJobs.getTotalPeople()<0) {
						//departure=startServiceTimePossible-registration;
						arrival=startServiceTimePossible-registration-loadDownloadTime;
					}
					else {
						//departure=startServiceTimePossible+loadDownloadTime;
						arrival=startServiceTimePossible;
					}

					iJobs.setarrivalTime(arrival);
					iJobs.setdepartureTime(departure);
					iJobs.setStartServiceTime(startServiceTimePossible);
					iJobs.setEndServiceTime(iJobs.getstartServiceTime()+iJobs.getReqTime());
					arrival=iJobs.getArrivalTime();

					if((i+1)==r.getListSubJobs().size()-1) {
						r.getListSubJobs().get(r.getListSubJobs().size()-1).setarrivalTime(arrivalLast);
						r.getListSubJobs().get(r.getListSubJobs().size()-1).setdepartureTime(departureLast);
					}


				}
				else {
					arrival=iJobs.getArrivalTime();
				}
			}
			waitingTime= computeW(r);
		}
	}


	private double computeW(Parts r) {
		double penalizationRoute=0;

		for(SubJobs j:r.getListSubJobs()) {
			double additionaltime=determineAdditionalTime(j,test);

			double delta=(j.getstartServiceTime()-(j.getArrivalTime()+additionaltime));

			if(delta>0) {
				penalizationRoute+=delta;
				//System.out.println(j.toString());
			}

			if(delta<0) {
				//System.out.println(j.toString());
			}

		}


		return penalizationRoute;
	}

	private double determineAdditionalTime(SubJobs j,Test test) {
		double additionalTime=0;
		if(j.getTotalPeople()<0) {
			additionalTime=timeDropOffBeforeService(j,test); //medical centre, patient, client
		}
		else {
			//additionalTime=timePickUpAfterService(j);
		}
		return additionalTime;
	}




	private double timeDropOffBeforeService(SubJobs j,Test test) {
		double additionalTime=0;
		//medical centre, patient, client
		if(j.isMedicalCentre()) {// medical centre
			//additionalTime=test.getRegistrationTime();
			additionalTime=test.getloadTimePatient()+test.getRegistrationTime();
		}
		else {
			if(j.isClient()) {// client
				additionalTime=test.getloadTimeHomeCareStaff();
			}
			else {//patient
				//	additionalTime=test.getloadTimePatient();

			}
		}
		return additionalTime;
	}

	private double computingServiceStartTime(double startServiceTimePossible, SubJobs iJobs) {
		double startTime=startServiceTimePossible;
		if(iJobs.isClient()) {
			if(startTime>=iJobs.getSoftStartTime() && startTime<=iJobs.getSoftEndTime()) {
				startServiceTimePossible=Math.max(startTime, iJobs.getSoftStartTime());

			}
		}
		else {
			if(startTime<=iJobs.getSoftEndTime()) {
				startServiceTimePossible=Math.max(startTime, iJobs.getstartServiceTime());

			}
		}
		return startServiceTimePossible;
	}

	private boolean computingFeasibility(double startServiceTimePossible, double departure, SubJobs iJobs) {
		boolean feasible= false;
		double startTime=startServiceTimePossible;
		if(iJobs.isClient()) {
			//if(startTime>=iJobs.getStartTime() && startTime<=iJobs.getEndTime()) {
			if(startTime>=iJobs.getSoftStartTime() && startTime<=iJobs.getSoftEndTime()) {
				startServiceTimePossible=Math.max(startTime, iJobs.getSoftStartTime());
				feasible= true;
			}
		}
		else {
			if(iJobs.isMedicalCentre() && iJobs.getTotalPeople()<0) {
				if(startTime<=iJobs.getSoftEndTime() && departure>iJobs.getSoftEndTime()) {
					startServiceTimePossible=Math.max(startTime, iJobs.getstartServiceTime());
					feasible= true;
				}
			}
			else {
				if(startTime>=iJobs.getSoftStartTime() && startTime<=iJobs.getSoftEndTime()) {
					startServiceTimePossible=Math.max(startTime, iJobs.getSoftStartTime());
					feasible= true;
				}
			}
		}
		return feasible;
	}

	private double computingloadDownload(SubJobs iJobs, Test test) {
		if(iJobs.isClient()) return test.getloadTimeHomeCareStaff();
		return test.getloadTimePatient();
	}


	private double computingTimeBefore(SubJobs iJobs, Test test) {
		double registration=0;

		if(iJobs.isMedicalCentre() && iJobs.getTotalPeople()<0) {
			registration=test.getRegistrationTime();
		}

		return registration;
	}

	private void insertingSoftConstraints(HashMap<String, SubJobs> backUp, ArrayList<Parts> poolParts, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {
		ArrayList<Jobs> missingJobs= checkingMissingJobs(poolParts);
		if(missingJobs.isEmpty()) {
			for(Jobs j: missingJobs) {
				backUp.put(j.getSubJobKey(), (SubJobs)j);
			}
		}

	}

	private ArrayList<Jobs> checkingMissingJobs(ArrayList<Parts> poolParts) {
		ArrayList<Jobs> missingJobs = new ArrayList<>();
		HashMap<String, Jobs> missing = new HashMap<>();
		HashMap<String, Jobs> listJobs = new HashMap<>();
		for(Couple c: dropoffHomeCareStaff.values()) {
			listJobs.put(c.getPresent().getSubJobKey(), c.getPresent());
			listJobs.put(c.getFuture().getSubJobKey(), c.getFuture());
		}
		for(Couple c: dropoffpatientMedicalCentre.values()) {
			listJobs.put(c.getPresent().getSubJobKey(), c.getPresent());
			listJobs.put(c.getFuture().getSubJobKey(), c.getFuture());
		}
		for(Couple c: pickpatientMedicalCentre.values()) {
			listJobs.put(c.getPresent().getSubJobKey(), c.getPresent());
			listJobs.put(c.getFuture().getSubJobKey(), c.getFuture());
		}

		for(Jobs j: listJobs.values()) {
			if(j.getSubJobKey().equals("D17")) {
				//System.out.println("Stop");
			}
			boolean assigned=false;
			for(Parts p: poolParts) {
				if(p.getDirectorySubjobs().containsKey(j.getSubJobKey())) {
					assigned=true;
					break;
				}
			}
			if(!assigned) {
				missing.put(j.getSubJobKey(), j);
			}
		}
		for(Jobs j: missing.values()) {
			missingJobs.add(j);
		}

		return missingJobs;

	}

	private Solution sortInsertionProcedurePartial(Solution s) {
		//		private  HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff 
		//		private  HashMap<String, Couple> dropoffpatientMedicalCentre= new HashMap<>();// hard time windows list of patient
		//		private  HashMap<String, Couple> pickpatientMedicalCentre= new HashMap<>();// soft time windows list of patient
		//		private  HashMap<String, Couple> pickUpHomeCareStaff= new HashMap<>();// soft time windows list of home care staff 

		ArrayList<Jobs> insertionOrder=sortingJobsList(s);
		HashMap<String,SubJobs> toInsert= new HashMap<String,SubJobs>();

		// pool of routes
		ArrayList<Parts> poolParts= poolPartscreationRoute();		


		do {
			for(Jobs j: insertionOrder) {
				boolean inserted= insertionJobSelectingRoute(poolParts,j,toInsert);

				if(inserted) {
					insertionOrder.remove(j);
					for(Jobs sb: insertionOrder){
						toInsert.put(sb.getSubJobKey(),(SubJobs) sb);
					}
					insertionOrder.clear();
					for(SubJobs sb: toInsert.values()){
						insertionOrder.add(sb);
					}
					toInsert.clear();
					insertionOrder.sort(Jobs.SORT_BY_STARTW);
					break;
				}
			}
		}while(!insertionOrder.isEmpty());

		mergingParts(poolParts);  // merging Parts
		ArrayList<Route> poolRoutes=insertingDepotConnections(poolParts);
		//mergingRoutes(poolRoutes);

		//slackTime(poolRoutes);
		stackingRoutes(poolRoutes);

		poolRoutes.sort(Route.SORT_BY_EarlyJob);
		//boolean goodSolution=validationSequenceRoutes(poolRoutes);


		Solution solution= new Solution ();
		for(Route route:poolRoutes) {
			if(!route.getPartsRoute().isEmpty()) {
				solution.getRoutes().add(route);}
		}

		return solution;
	}


	private boolean insertionJobSelectingRoute(ArrayList<Parts> poolParts, Jobs j, HashMap<String, SubJobs> toInsert) {
		boolean inserted= false;
		ArrayList<Parts> bestRoutes=selectionBestParst(j,poolParts);
		Parts changing=null;
		for(Parts p: bestRoutes) {
			inserted=inseringRoute(j,p);

			if(inserted) {
				changing=p;
				break;
			}
		}
		if(!inserted) {
			Parts p= new Parts();
			inserted=inseringRoute(j,p);
			poolParts.add(p);
			changing=p;
		}

		// update times for future
		return inserted;
	}

	private ArrayList<Jobs> sortingJobsList(Solution s) {
		ArrayList<Jobs> insertionOrder = new ArrayList<Jobs>();
		for(Route r:s.getRoutes()) {
			for(Parts p:r.getPartsRoute()) {
				for(SubJobs j:p.getListSubJobs()) {
					if(j.getId()!=1) {
						insertionOrder.add(j);
					}
				}
			}
		}
		int sortCriterion=rn.nextInt(4)+1;// 1 distance depot // 2 early TW // 3 size 
		switch(sortCriterion){
		case 1:  
			computingAdditionalCriteria(insertionOrder, 1);
			insertionOrder.sort(Jobs.SORT_DistaceLastConnectedNode);
			break;
		case 2:  insertionOrder.sort(Jobs.SORT_BY_STARTW);
		break;
		case 3:  insertionOrder.sort(Jobs.TWSIZE_Early);
		break;
		case 4:  Collections.shuffle(insertionOrder,rn);
		break;
		}
		return insertionOrder;
	}


	private ArrayList<Parts> assigmentShifts(ArrayList<Route> poolRoutes, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		for(Route r:poolRoutes) {
			for(SubJobs j:r.getSubJobsList()) {
				if(j.isClient() || j.isMedicalCentre()) {
					if(j.getTotalPeople()<0) {
						list.add(j);
					}

				}
			}
		}
		ArrayList<ArrayList<SubJobs>> jobsQualification= new ArrayList<>();

		for(int i=0;i<4;i++) { // calling jobs <- qualification levels
			ArrayList<SubJobs> jobsList= selectingJobsQualification(list,i);
			jobsQualification.add(jobsList);
		}

		// calling medical staff
		// HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {
		ArrayList<ArrayList<Parts>>  aux= null;//auxiliar
		ArrayList<ArrayList<Parts>>  shiftParamedics= assigmentJobsQualification(0,jobsQualification.get(0),aux, dropoffHomeCareStaff2, pickupHomeCareStaff2, dropoffpatientMedicalCentre2, pickpatientMedicalCentre2); // including the list of jobs
		ArrayList<ArrayList<Parts>>  shifthomeCareStaffQ3= assigmentJobsQualification(3,jobsQualification.get(3),aux ,dropoffHomeCareStaff2, pickupHomeCareStaff2, dropoffpatientMedicalCentre2, pickpatientMedicalCentre2); // including the list of jobs
		ArrayList<ArrayList<Parts>>  shifthomeCareStaffQ2= assigmentJobsQualification(2,jobsQualification.get(2),shifthomeCareStaffQ3, dropoffHomeCareStaff2, pickupHomeCareStaff2, dropoffpatientMedicalCentre2, pickpatientMedicalCentre2); // including the list of jobs

		ArrayList<ArrayList<Parts>>  shifthomeCareStaffQ1= assigmentJobsQualification(1,jobsQualification.get(1),shifthomeCareStaffQ2, dropoffHomeCareStaff2, pickupHomeCareStaff2, dropoffpatientMedicalCentre2, pickpatientMedicalCentre2); // including the list of jobs

		// se evaluan los posibles downgradings. (1) trabajos de qualification 1 son asignados a home care staff de qualification 2 (2) trabajos de qualification 2 son asignados a home care staff de qualification 1
		downgradings(shifthomeCareStaffQ3,shifthomeCareStaffQ2,shifthomeCareStaffQ1);


		// test

		//

		// consolidation parts
		ArrayList<Parts> listShift= new ArrayList<Parts>(); 
		// paramedics
		for(ArrayList<Parts> p:shiftParamedics) {
			Parts consolidation= new Parts();
			for(Parts pp: p) {
				for(SubJobs sj:pp.getListSubJobs()) {
					consolidation.getListSubJobs().add(sj);
					consolidation.getDirectorySubjobs().put(sj.getSubJobKey(), sj);
				}
			}
			listShift.add(consolidation);
		}

		// home care staff cualification 1
		for(ArrayList<Parts> p:shifthomeCareStaffQ1) {
			Parts consolidation= new Parts();
			for(Parts pp: p) {
				for(SubJobs sj:pp.getListSubJobs()) {
					consolidation.getListSubJobs().add(sj);
					consolidation.getDirectorySubjobs().put(sj.getSubJobKey(), sj);
				}
			}
			listShift.add(consolidation);
		}

		// home care staff cualification 2
		for(ArrayList<Parts> p:shifthomeCareStaffQ2) {
			Parts consolidation= new Parts();
			for(Parts pp: p) {
				for(SubJobs sj:pp.getListSubJobs()) {
					consolidation.getListSubJobs().add(sj);
					consolidation.getDirectorySubjobs().put(sj.getSubJobKey(), sj);
				}
			}
			listShift.add(consolidation);
		}

		// home care staff cualification 3
		for(ArrayList<Parts> p:shifthomeCareStaffQ3) {
			Parts consolidation= new Parts();
			for(Parts pp: p) {
				for(SubJobs sj:pp.getListSubJobs()) {
					consolidation.getListSubJobs().add(sj);
					consolidation.getDirectorySubjobs().put(sj.getSubJobKey(), sj);
				}
			}
			listShift.add(consolidation);
		}



		return listShift;
	}



	private void downgradings(ArrayList<ArrayList<Parts>> shifthomeCareStaffQ3, ArrayList<ArrayList<Parts>> shifthomeCareStaffQ2,
			ArrayList<ArrayList<Parts>> shifthomeCareStaffQ1) {
		fromHighToLowQualification(shifthomeCareStaffQ1,shifthomeCareStaffQ2);
		fromHighToLowQualification(shifthomeCareStaffQ2,shifthomeCareStaffQ3);



	}

	private void fromHighToLowQualification(ArrayList<ArrayList<Parts>> shifthomeCareStaffQ1,
			ArrayList<ArrayList<Parts>> shifthomeCareStaffQ2) {
		// en caso de que no se use personal
		for(ArrayList<Parts> lowQualification: shifthomeCareStaffQ1) { // jobs qualification 1 -> home care staff cualification 2
			for(ArrayList<Parts> highQualification: shifthomeCareStaffQ2) { // jobs qualification 1 -> home care staff cualification 2
				if(!lowQualification.isEmpty()) {
					for(Parts p: lowQualification) { // intentando de insertar parte por parte
						SubJobs firstJobLow=p.getListSubJobs().get(0);
						SubJobs lastJobLow=p.getListSubJobs().get(p.getListSubJobs().size()-1);
						SubJobs firstJobHigh=highQualification.get(0).getListSubJobs().get(0);
						SubJobs lastJobHigh=highQualification.get(highQualification.size()-1).getListSubJobs().get(highQualification.get(highQualification.size()-1).getListSubJobs().size()-1);
						if(Math.max(lastJobLow.getDepartureTime(), lastJobLow.getendServiceTime())<=Math.min(firstJobHigh.getArrivalTime(), firstJobHigh.getstartServiceTime())) { // insert part early  part(low)--- part(high-high)
							double start =Math.min(firstJobLow.getstartServiceTime(),firstJobLow.getArrivalTime());
							double end= Math.max(lastJobHigh.getendServiceTime(),lastJobHigh.getDepartureTime());

							if(Math.abs(end-start)<=test.getWorkingTime()) { // working hours
								highQualification.add(0,p);
							}
						}
						else {
							if(Math.max(lastJobHigh.getDepartureTime(), lastJobHigh.getendServiceTime())<=Math.min(firstJobLow.getArrivalTime(), firstJobLow.getstartServiceTime())) { // insert part early  part(low)--- part(high-high)
								double start =Math.min(firstJobHigh.getstartServiceTime(),firstJobHigh.getArrivalTime());
								double end= Math.max(lastJobLow.getendServiceTime(),lastJobLow.getDepartureTime());							
								if(Math.abs(end-start)<=test.getWorkingTime()) { // working hours
									highQualification.add(p);
								}
							}
						}
					}
					for(Parts p: highQualification) {
						if(lowQualification.contains(p)) {
							lowQualification.remove(p);
						}

					}
				}
				else {
					break;
				}
			}
		}
		// ArrayList<ArrayList<Parts>> shifthomeCareStaffQ1, ArrayList<ArrayList<Parts>> shifthomeCareStaffQ2
		ArrayList<ArrayList<Parts>> shiftcopyQ1= new ArrayList<ArrayList<Parts>> ();
		for(ArrayList<Parts> p:shifthomeCareStaffQ1) {
			if(!p.isEmpty()) {
				shiftcopyQ1.add(p);}
		}
		shifthomeCareStaffQ1.clear();
		for(ArrayList<Parts> p:shiftcopyQ1) {
			if(!p.isEmpty()) {
				shifthomeCareStaffQ1.add(p);}
		}
		ArrayList<ArrayList<Parts>> shiftcopyQ2= new ArrayList<ArrayList<Parts>> ();
		for(ArrayList<Parts> p:shifthomeCareStaffQ2) {
			if(!p.isEmpty()) {
				shiftcopyQ2.add(p);}
		}
		shifthomeCareStaffQ2.clear();
		for(ArrayList<Parts> p:shiftcopyQ2) {
			if(!p.isEmpty()) {
				shifthomeCareStaffQ2.add(p);}
		}

	}

	private ArrayList<ArrayList<Parts>> assigmentJobsQualification(int i, ArrayList<SubJobs> jobsList, ArrayList<ArrayList<Parts>> aux, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {
		ArrayList<ArrayList<Parts>> shiftQualification= new ArrayList<>();
		jobsList.sort(Jobs.SORT_BY_STARTW);
		// Assignment
		for(SubJobs j:jobsList) { // iterating over the list of jobs

			boolean inserted=false;
			if(j.getSubJobKey().equals("D5174")) {
				System.out.println("Stop");

			}
			if(j.getSubJobKey().equals("D24")) {
				System.out.println("Stop");

			}
			for(ArrayList<Parts> shift:shiftQualification) { // iterating over the list of medical 
				// private ArrayList<ArrayList<SubJobs>> assigmentJobsQualification(int i, ArrayList<SubJobs> jobsList, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {

				inserted=assigningJobsToMedicalStaff(j,shift, dropoffHomeCareStaff2,  pickupHomeCareStaff2,  dropoffpatientMedicalCentre2,  pickpatientMedicalCentre2);
				if(inserted) {
					break;
				}
			}
			if(!inserted && aux!=null) { // se intenta asignar a los de qualification 3
				int control=inp.gethomeCareStaffInf().get(i).getQuantity();
				if(control<=aux.size()) {
					for(ArrayList<Parts> shift:aux) { // iterating over the list of medical 
						inserted=assigningJobsToMedicalStaff(j,shift, dropoffHomeCareStaff2,  pickupHomeCareStaff2,  dropoffpatientMedicalCentre2,  pickpatientMedicalCentre2);
						if(inserted) {
							break;
						}
					}}

			}
			if(!inserted) { // // se crea un nuevo turno
				ArrayList<Parts> shift = new ArrayList<Parts> ();
				shiftQualification.add(shift);
				inserted=assigningJobsToMedicalStaff(j,shift, dropoffHomeCareStaff2,  pickupHomeCareStaff2,  dropoffpatientMedicalCentre2,  pickpatientMedicalCentre2);
			}
		}
		// Merging Possibles parts
		merging(shiftQualification);

		for(ArrayList<Parts> p:shiftQualification) {
			for(Parts pp: p) {
				pp.setQualificationParts(i);
			}
		}
		return shiftQualification;
	}

	private void merging(ArrayList<ArrayList<Parts>> shiftQualification) {
		ArrayList<ArrayList<Parts>> copy= new ArrayList<>();
		for(ArrayList<Parts> shift1: shiftQualification) {
			copy.add(shift1);
		}
		for(ArrayList<Parts> shift1: copy) {
			for(ArrayList<Parts> shift2: copy) {
				if(shift1!=shift2 && !shift1.isEmpty() && !shift2.isEmpty() ) {
					Parts lastPart=shift1.get(shift1.size()-1);
					Parts firstPart=shift2.get(0);	

					if(Math.max(lastPart.getListSubJobs().get(lastPart.getListSubJobs().size()-1).getendServiceTime(), lastPart.getListSubJobs().get(lastPart.getListSubJobs().size()-1).getDepartureTime())<=Math.min(firstPart.getListSubJobs().get(0).getstartServiceTime(), firstPart.getListSubJobs().get(0).getArrivalTime())) {
						double end=Math.max(firstPart.getListSubJobs().get(firstPart.getListSubJobs().size()-1).getendServiceTime(),firstPart.getListSubJobs().get(firstPart.getListSubJobs().size()-1).getDepartureTime());
						double start=Math.min(shift1.get(0).getListSubJobs().get(0).getstartServiceTime(),shift1.get(0).getListSubJobs().get(0).getArrivalTime());
						if(Math.abs(end-start)<=test.getWorkingTime()) {			
							//if(firstPart.getListSubJobs().get(firstPart.getListSubJobs().size()-1).getendServiceTime()-lastPart.getListSubJobs().get(0).getstartServiceTime()<=test.getWorkingTime()) {
							for(Parts j: shift2) {
								shift1.add(j);
							}
							shiftQualification.remove(shift2);
							shift2.clear();
						}
					}
				}
			}
		}
	}

	private boolean assigningJobsToMedicalStaff(SubJobs j, ArrayList<Parts> shift, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {
		boolean inserted=false;
		// HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2
		if(j.getSubJobKey().equals("D43")) {
			//System.out.println("Stop");

		}
		if(j.getSubJobKey().equals("D24")) {
			//System.out.println("Stop");

		}


		Parts p= callingParts(j, dropoffHomeCareStaff2,  pickupHomeCareStaff2,  dropoffpatientMedicalCentre2,  pickpatientMedicalCentre2);
		if(shift.isEmpty()) {
			if(j.getSubJobKey().equals("D43")) {
				//System.out.println("Stop");

			}
			if(j.getSubJobKey().equals("D28")) {
				//System.out.println("Stop");

			}
			inserted=true;

			shift.add(p);	
		}
		else { // se intenta insertar al final de la secuencia
			if(shift.get(shift.size()-1).getListSubJobs().get(shift.get(shift.size()-1).getListSubJobs().size()-1).getendServiceTime()<p.getListSubJobs().get(0).getstartServiceTime()) {
				if(shift.get(shift.size()-1).getListSubJobs().get(shift.get(shift.size()-1).getListSubJobs().size()-1).getDepartureTime()<p.getListSubJobs().get(0).getArrivalTime()) {

					double start =Math.min(shift.get(0).getListSubJobs().get(0).getstartServiceTime(),shift.get(0).getListSubJobs().get(0).getArrivalTime());
					double end= Math.max(p.getListSubJobs().get(p.getListSubJobs().size()-1).getendServiceTime(),p.getListSubJobs().get(p.getListSubJobs().size()-1).getDepartureTime());
					if(end-start<test.getWorkingTime()) {
						if(j.getSubJobKey().equals("D43")) {
							//System.out.println("Stop");

						}
						if(j.getSubJobKey().equals("D28")) {
							//System.out.println("Stop");

						}
						inserted=true;
						shift.add(p);
					}
				}	
			}
		}
		double start =Math.min(shift.get(0).getListSubJobs().get(0).getstartServiceTime(),shift.get(0).getListSubJobs().get(0).getArrivalTime());
		double end =Math.min(shift.get(shift.size()-1).getListSubJobs().get(shift.get(shift.size()-1).getListSubJobs().size()-1).getstartServiceTime(),shift.get(shift.size()-1).getListSubJobs().get(shift.get(shift.size()-1).getListSubJobs().size()-1).getArrivalTime());
		if(end-start>test.getWorkingTime()) {
			//System.out.println("Stop");
		}
		if(end-start<test.getWorkingTime()) {
			//System.out.println("Stop");
		}
		return inserted;
	}

	private Parts callingParts(SubJobs j, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2,
			HashMap<String, Couple> pickpatientMedicalCentre2) {
		// los trabajos tipos "j"  son todos drop-off
		Parts p= new Parts();
		if(j.isClient()) {
			Couple c=dropoffHomeCareStaff2.get(j.getSubJobKey());
			p.getListSubJobs().add((SubJobs)c.getPresent());
			p.getListSubJobs().add((SubJobs)c.getFuture());
			p.getDirectorySubjobs().put(c.getPresent().getSubJobKey(), (SubJobs)c.getPresent());
			p.getDirectorySubjobs().put(c.getFuture().getSubJobKey(), (SubJobs)c.getFuture());
		}
		else { // is a medical centre
			Couple c1=dropoffpatientMedicalCentre2.get(j.getSubJobKey());


			p.getListSubJobs().add((SubJobs)c1.getPresent());
			p.getListSubJobs().add((SubJobs)c1.getFuture());
			p.getDirectorySubjobs().put(c1.getPresent().getSubJobKey(), (SubJobs)c1.getPresent());
			p.getDirectorySubjobs().put(c1.getFuture().getSubJobKey(), (SubJobs)c1.getFuture());
			String dropOffHome="D"+j.getIdUser();
			Couple c2=pickpatientMedicalCentre2.get(dropOffHome);
			p.getListSubJobs().add((SubJobs)c2.getPresent());
			p.getListSubJobs().add((SubJobs)c2.getFuture());
			p.getDirectorySubjobs().put(c2.getPresent().getSubJobKey(), (SubJobs)c2.getPresent());
			p.getDirectorySubjobs().put(c2.getFuture().getSubJobKey(), (SubJobs)c2.getFuture());
		}
		return p;
	}

	private ArrayList<SubJobs> selectingJobsQualification(ArrayList<SubJobs> list, int i) {
		ArrayList<SubJobs> jobsQualification= new ArrayList<SubJobs>();
		for(SubJobs j: list) {
			if(j.getReqQualification()==i) {
				jobsQualification.add(j);
			}
		}
		return jobsQualification;
	}

	private boolean validationSequenceRoutes(ArrayList<Route> poolRoutes) {	
		boolean feasible= false;
		ArrayList<Route> wrongArrivalDepartureTime=checkingDepartureTimesVSDepartureTimes(poolRoutes); 
		ArrayList<Route> wrongDepartureArrivalTime=checkingArrivalTimesVSDepartureTimes(poolRoutes); // print routes con problemas en el tiempo de departure y arrival
		ArrayList<Route> wrongHorasInicioServicio=checkingStartServiceTimes(poolRoutes); // print routes con problemas en el tiempo de departure y arrival
		if(wrongDepartureArrivalTime.isEmpty() && wrongHorasInicioServicio.isEmpty() && wrongArrivalDepartureTime.isEmpty()) {
			feasible= true;
		}
		boolean allJobs=checkingJobs(poolRoutes);




		if(allJobs && feasible) return true;
		return false;
	}

	private ArrayList<Route> checkingDepartureTimesVSDepartureTimes(ArrayList<Route> poolRoutes) {
		ArrayList<Route> routes  = new ArrayList<Route>();
		for(Route r: poolRoutes) {
			for(SubJobs j: r.getSubJobsList()) {
				if(j.getArrivalTime()>j.getDepartureTime()) {
					routes.add(r);
					break;
				}
			}
		}
		return routes;
	}

	private boolean checkingJobs(ArrayList<Route> poolRoutes) {
		boolean feasible= true;
		//		private  HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff 
		//		private  HashMap<String, Couple> dropoffpatientMedicalCentre= new HashMap<>();// hard time windows list of patient
		//		private  HashMap<String, Couple> pickpatientMedicalCentre= new HashMap<>();// soft time windows list of patient
		//		private  HashMap<String, Couple> pickUpHomeCareStaff= new HashMap<>();// soft time windows list of home care staff 

		for(Couple c:dropoffHomeCareStaff.values()) {
			SubJobs present= new SubJobs(c.getPresent());


			Route r=selectionRoute(present,poolRoutes);
			if(r==null) {
				feasible= false;
				break;
			}
			SubJobs future= new SubJobs(c.getFuture());

			r=selectionRoute(future,poolRoutes);
			if(r==null) {
				feasible= false;
				break;
			}
		}

		if(feasible) {
			for(Couple c:dropoffpatientMedicalCentre.values()) {
				SubJobs present= new SubJobs(c.getPresent());
				Route r=selectionRoute(present,poolRoutes);
				if(r==null) {
					feasible= false;
					break;
				}
				SubJobs future= new SubJobs(c.getFuture());

				r=selectionRoute(future,poolRoutes);
				if(r==null) {
					feasible= false;
					break;
				}
			}
		}
		if(feasible) {
			for(Couple c:pickpatientMedicalCentre.values()) {
				SubJobs present= new SubJobs(c.getPresent());

				Route r=selectionRoute(present,poolRoutes);
				if(r==null) {
					feasible= false;
					break;
				}
				SubJobs future= new SubJobs(c.getFuture());

				r=selectionRoute(future,poolRoutes);
				if(r==null) {
					feasible= false;
					break;
				}
			}
		}
		return feasible;
	}

	private ArrayList<Route> checkingStartServiceTimes(ArrayList<Route> poolRoutes) {
		ArrayList<Route> routes  = new ArrayList<Route>();
		boolean correct= false;
		for(Route r: poolRoutes) {
			correct=checkingStartTime(r);
			if(!correct) {
				routes.add(r);
			}
		}
		return routes;
	}

	private boolean checkingStartTime(Route r) {
		boolean correct= true;
		for(SubJobs j:r.getSubJobsList()) {
			if(j.getTotalPeople()<0) {
				if(j.isMedicalCentre() || j.isClient()) {
					int index=-1;
					if(j.isMedicalCentre()) {
						index=j.getIdUser()-1;
					}
					else {
						index=j.getId()-1;

					}
					if(j.getstartServiceTime()>inp.getNodes().get(index).getEndTime() || j.getstartServiceTime()<inp.getNodes().get(index).getStartTime()) {
						correct= false;
					}
				}
			}
		}
		return correct;
	}

	private ArrayList<Route> checkingArrivalTimesVSDepartureTimes(ArrayList<Route> poolRoutes) {
		ArrayList<Route> routes  = new ArrayList<Route>();
		boolean correct= false;
		for(Route r: poolRoutes) {
			correct=checkingDepartureArrival(r);
			if(!correct) {
				routes.add(r);
			}
		}
		return routes;
	}

	private boolean checkingDepartureArrival(Route r) {
		boolean correct= true;
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		for(int p=1;p<r.getPartsRoute().size()-1;p++) {
			for(SubJobs j:r.getPartsRoute().get(p).getListSubJobs()) {
				list.add(j);
			}	
		}
		if(!list.isEmpty()) {
			for(int i= 1;i<list.size();i++) {
				SubJobs a=list.get(i-1);
				SubJobs b=list.get(i);
				double travelTime=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
				if(a.getId()!=b.getId()) {
					if(a.getDepartureTime()>b.getArrivalTime()) {
						correct= false;
						break;
					}}
			}
		}
		return correct;
	}

	private void stackingRoutes(ArrayList<Route> poolRoutes) {
		ArrayList<Route> copyRoute= new ArrayList<Route>();
		for(Route r: poolRoutes) {
			copyRoute.add(r);
		}
		copyRoute.sort(Route.SORT_BY_EarlyJob);
		for(Route r1:copyRoute ) {
			for(Route r2:copyRoute ) {

				if(r1!=r2 && !r1.getSubJobsList().isEmpty() && !r2.getSubJobsList().isEmpty()) {// it is inserted at the end of the route.
					SubJobs depotReturning=r1.getPartsRoute().get(r1.getPartsRoute().size()-1).getListSubJobs().get(0);
					SubJobs depotGoingOut=r2.getPartsRoute().get(0).getListSubJobs().get(0);
					if(depotReturning.getArrivalTime()<depotGoingOut.getArrivalTime()) {
						for(Parts p:r2.getPartsRoute()) {
							r1.getPartsRoute().add(p);
							r1.getSubJobsList().clear();
							r1.getJobsDirectory().clear();
							for(int i=1;i<r1.getPartsRoute().size()-1;i++) {
								for(SubJobs j:r1.getPartsRoute().get(i).getListSubJobs()) {
									if(j.getId()!=1) {
										r1.getSubJobsList().add(j);
										r1.getJobsDirectory().put(j.getSubJobKey(), j);}
								}
							}
						}
						r2.getPartsRoute().clear();
						r2.getSubJobsList().clear();
					}
				}

			}

		}
		poolRoutes.clear();

		for(Route r: copyRoute) {
			if(!r.getPartsRoute().isEmpty()) {
				poolRoutes.add(r);
			}

		}
		updatingConnectionsList(poolRoutes);// Update list of connections (Parts and in general route)
		updatingSubJobsList(poolRoutes);// Update list of Subjobs in the route

	}

	private void updatingSubJobsList(ArrayList<Route> poolRoutes) {
		// Desde las partes actualizar la lista de SubJobs
		for(Route r:poolRoutes) {
			updatingSubJobsListParts(r);

			updatingSubJobsListRoute(r);
		}
		// directorio y lista de subjobs

	}

	private void updatingSubJobsListRoute(Route r) {
		r.getJobsDirectory().clear();
		r.getSubJobsList().clear();
		for(Parts p:r.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {
				if(j.getId()!=1) {
					r.getSubJobsList().add(j);
					r.getJobsDirectory().put(j.getSubJobKey(), j);
				}
			}
		}

	}

	private void updatingSubJobsListParts(Route r) {
		// updating directory
		for(Parts p: r.getPartsRoute()) {
			p.getDirectorySubjobs().clear();
			for(SubJobs sj:p.getListSubJobs() ) {
				p.getDirectorySubjobs().put(sj.getSubJobKey(), sj);
			}
		}

	}

	private void updatingConnectionsList(ArrayList<Route> poolRoutes) {
		for(Route r: poolRoutes) {
			updatingConnectionsParts(r);// Update list of connections Parts
			updatingConnectionsRoutes(r);		// Update list of connections route

		}
	}

	private void updatingConnectionsRoutes(Route r) {
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();
		for(Parts p:r.getPartsRoute()) {
			for(SubJobs sj:p.getListSubJobs()) {
				subJobsList.add(sj);
			}	
		}
		for(int i=1;i<subJobsList.size();i++) {
			SubJobs origen=subJobsList.get(i-1);
			SubJobs end=subJobsList.get(i);
			if(origen.getId()!=end.getId()) {
				Edge e= new Edge(origen, end, inp,test);
				r.getEdges().put(e.getEdgeKey(), e);
			}
		}

	}

	private void updatingConnectionsParts(Route r) {

		for(Parts p:r.getPartsRoute()) {
			p.getDirectoryConnections().clear();
			for(int i=1;i<p.getListSubJobs().size();i++) {
				SubJobs origen=p.getListSubJobs().get(i-1);
				SubJobs end=p.getListSubJobs().get(i);
				Edge e= new Edge(origen, end, inp,test);
				p.getDirectoryConnections().put(e.getEdgeKey(), e);
			}	
		}


	}

	private void mergingParts(ArrayList<Parts> poolParts) {
		mergingCompletePart(poolParts);

		for(Parts p2:poolParts) {
			ArrayList<SubJobs> sequence = new ArrayList<SubJobs>();
			for(SubJobs job:p2.getListSubJobs()) {
				sequence.add(job);
			}
			p2.getListSubJobs().clear();
			p2.setListSubJobs(sequence, inp, test);
		}
	}



	private void orderingSequence(ArrayList<SubJobs> jobs) { 
		ArrayList<SubJobs> jobsCopy= new ArrayList<SubJobs>();	// copy
		for(SubJobs j: jobs) {
			jobsCopy.add(new SubJobs(j));
		}
		ArrayList<SubJobs> jobsSequence= new ArrayList<SubJobs>();
		jobsSequence.add(jobsCopy.get(0));
		jobsCopy.remove(jobsCopy.get(0));
		boolean inserting=true;
		while(inserting) {
			if(jobsCopy.isEmpty()) {
				inserting=false;
			}
			for(int index=0;index<jobsCopy.size();index++) {
				SubJobs a=jobsSequence.get(jobsSequence.size()-1);
				SubJobs b=jobsCopy.get(index);
				if(!jobsSequence.contains(b)) {
					double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
					double test=a.getStartTime()-tv+b.getloadUnloadRegistrationTime()+b.getloadUnloadTime();
					if(test<=b.getEndTime()) {
						//if(test<=b.getEndTime() && test>=b.getStartTime()) {
						ArrayList<SubJobs> newSequence= new ArrayList<SubJobs>();	// copy
						for(SubJobs j: jobsSequence) {
							newSequence.add(new SubJobs(j));
						}
						newSequence.add(new SubJobs(b));
						if(vehicleCapacityPart(newSequence)) {
							jobsSequence.add(b);
							jobsCopy.remove(b);
							break;
						}
						else {
							inserting=false;
							jobs.clear();
							break;
						}
					}
					else {
						inserting=false;
						jobs.clear();
						break;
					}
				}
				if(jobsCopy.isEmpty()) {
					inserting=false;
				}
			}
		}
		//.checkingSequenceVehicleCapacity(sequence)
	}



	private void mergingCompletePart(ArrayList<Parts> poolParts) {
		ArrayList<Parts> partsCopy = new ArrayList<Parts>();
		for(Parts p1:poolParts) {
			partsCopy.add(new Parts(p1));
		}
		ArrayList<Parts> parts = null;
		for(int i=0;i<partsCopy.size();i++) {
			Parts p1=partsCopy.get(i);
			for(int j=0;j<partsCopy.size();j++) { // siempre se pone al final de cada parte
				Parts p2=partsCopy.get(j);
				if(p1!=p2 && !p1.getListSubJobs().isEmpty() && !p2.getListSubJobs().isEmpty()) {// solo se mezclan las partes que son diferentes entre ellas

					SubJobs lastP1=p1.getListSubJobs().get(p1.getListSubJobs().size()-1);
					SubJobs firstP2=p2.getListSubJobs().get(0);
					if(lastP1.getDepartureTime()<firstP2.getArrivalTime()) { // siempre se pone al final de cada parte
						double travelTime=inp.getCarCost().getCost(lastP1.getId()-1, firstP2.getId()-1);
						double possibleArrivalTime=lastP1.getDepartureTime()+travelTime+firstP2.getloadUnloadRegistrationTime()+firstP2.getloadUnloadTime();

						if(lastP1.getDepartureTime()+travelTime+firstP2.getloadUnloadRegistrationTime()+firstP2.getloadUnloadTime()<=firstP2.getEndTime() && lastP1.getDepartureTime()+travelTime+firstP2.getloadUnloadRegistrationTime()+firstP2.getloadUnloadTime()>=firstP2.getStartTime()) {						
							parts = new ArrayList<Parts>();
							parts.add(poolParts.get(i));
							parts.add(poolParts.get(j));

							ArrayList<SubJobs> sequence = new ArrayList<SubJobs>();
							for(Parts p:parts) {
								for(SubJobs sj: p.getListSubJobs()) {
									sequence.add(sj);
								}	
							}
							sequence.sort(Jobs.SORT_BY_STARTW);
							boolean inserted=testing(sequence);

							if(inserted) {
								for(int index=1;index<sequence.size();index++) {
									SubJobs a=sequence.get(index-1);
									SubJobs b=sequence.get(index);
									double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
									double calculatedStartTime=0;
									double possibleStartTime=0;
									if(b.isMedicalCentre() ) {
										if(b.getTotalPeople()<0) {
											calculatedStartTime=(a.getDepartureTime()+tv+test.getRegistrationTime());
											possibleStartTime=Math.max(b.getEndTime(), calculatedStartTime);
										}
										else {
											calculatedStartTime=(a.getDepartureTime()+tv);
											possibleStartTime=Math.max(b.getStartTime(), calculatedStartTime);
										}
									}
									else {
										if(b.isClient()) {
											calculatedStartTime=(a.getDepartureTime()+tv+test.getloadTimeHomeCareStaff());
										}
										else {
											calculatedStartTime=(a.getDepartureTime()+tv+test.getloadTimePatient());
										}
										possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);
									}

									b.setStartServiceTime(possibleStartTime);
									b.setarrivalTime(a.getDepartureTime()+tv);

									if(b.isClient()) {
										if(b.getTotalPeople()<0) {
											b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
											b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());}
										else {
											b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
											b.setEndServiceTime(b.getDepartureTime());
										}
									}
									else {
										if(b.getTotalPeople()<0) {
											b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
											b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());}
										else {
											b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
											b.setEndServiceTime(b.getDepartureTime());
										}
									}
								}

								// revisar la secuencia de las horas
								p2.getListSubJobs().clear();
								break;
							}
						}
					}

				}
			}
			if(parts!=null) {
				ArrayList<SubJobs> sequence = new ArrayList<SubJobs>();
				for(Parts p2:parts) {
					for(SubJobs job:p2.getListSubJobs()) {
						sequence.add(job);
					}
				}
				p1.getListSubJobs().clear();
				p1.setListSubJobs(sequence, inp, test);
			}



		}
		ArrayList<Parts> copy= new ArrayList<Parts>();
		for(Parts p: poolParts) {
			if(!p.getListSubJobs().isEmpty()) {
				copy.add(p);
			}
		}
		poolParts.clear();
		for(Parts p: copy) {
			poolParts.add(p);
		}

	}

	private HashMap<String, Couple> selectingHomeCareStaffPickUpCouple(HashMap<String, Couple> dropoffHomeCareStaff2) {
		HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff 
		for(Couple c:dropoffHomeCareStaff2.values()) {
			SubJobs startNode=new SubJobs(c.getPresent());
			SubJobs endNode=new SubJobs(c.getFuture());
			Couple paar=new Couple(startNode,endNode);
			// adjusting time windows
			//
			//   startNode.setStartTime(Math.max(startNode.getStartTime()-test.getCumulativeWaitingTime(), 0));
			//endNode.setEndTime(endNode.getStartTime()+test.getCumulativeWaitingTime());
			dropoffHomeCareStaff.put(endNode.getSubJobKey(), paar);
		}
		return dropoffHomeCareStaff;
	}

	private boolean insertionJobSelectingRoute(ArrayList<Parts> poolParts, Jobs j, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff, HashMap<String, SubJobs> toInsert, boolean hardConstraints) {
		boolean inserted= false;
		ArrayList<Parts> bestRoutes=selectionBestParst(j,poolParts);
		Parts changing=null;

		for(Parts p: bestRoutes) {
			if(p.getDirectorySubjobs().containsKey("D4961")) {
				System.out.println("Sol");
			}
			if(p.getDirectorySubjobs().containsKey("P59") && j.getSubJobKey().equals("D4568")) {
				//System.out.println("Sol");
			}
			if( j.getSubJobKey().equals("D4568")) {
				//System.out.println("Sol");
			}
			inserted=inseringRoute(j,p,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,pickupHomeCareStaff,hardConstraints);
			if(inserted) {
				if(p.getDirectorySubjobs().containsKey("D4961")) {
					System.out.println("Sol");
				}
				if(p.getDirectorySubjobs().containsKey("D41")) {
					//System.out.println("Sol");
				}
				changing=p;
				break;
			}
		}
		if(!inserted) {
			Parts p= new Parts();
			inserted=inseringRoute(j,p,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,pickupHomeCareStaff,hardConstraints);
			poolParts.add(p);
			changing=p;
		}

		updatingTimesCouples(poolParts,changing,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,toInsert);


		// update times for future
		return inserted;
	}

	private void updatingTimesCouples(ArrayList<Parts> poolParts, Parts changing, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, SubJobs> toInsert) {
		// new elements to insert
		generatingNextNodesToInsert(poolParts, changing, dropoffHomeCareStaff2,dropoffpatientMedicalCentre2, pickpatientMedicalCentre2, toInsert);
		// updating times other routes


	}



	private void calculatingTW(ArrayList<Parts> poolParts, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2,
			HashMap<String, SubJobs> toInsert) {

		// list inserted jobs
		HashMap<String, SubJobs> listKJobs= new HashMap<String, SubJobs>();
		for(Parts changing: poolParts) {
			for(SubJobs inRoute:changing.getListSubJobs()) {
				listKJobs.put(inRoute.getSubJobKey(), inRoute);
			}
		}
		for(Parts changing: poolParts) {
			for(SubJobs inRoute:changing.getListSubJobs()) {
				if(inRoute.getSubJobKey().equals("D17")){
					//System.out.println("Stop");
				}
				if(inRoute.isMedicalCentre() && inRoute.getTotalPeople()<0) {// option 1: drop off medical centre
					Couple part1=dropoffpatientMedicalCentre2.get(inRoute.getSubJobKey());

					String nextPart="D"+inRoute.getIdUser();

					Couple part2=pickpatientMedicalCentre2.get(nextPart);
					if(part2==null) {
						//System.out.println("Stop");
					}
					SubJobs present= new SubJobs(part2.getPresent());
					present.setloadUnloadRegistrationTime(0);
					SubJobs future=new SubJobs(part2.getFuture());
					if(!listKJobs.containsKey(present.getSubJobKey())) {
						double startServiceTime=inRoute.getendServiceTime();
						present.setStartTime(startServiceTime);
						present.setEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
						present.setSoftStartTime(startServiceTime);
						present.setSoftEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
						toInsert.put(present.getSubJobKey(), present);
					}
					if(!listKJobs.containsKey(future.getSubJobKey())) {
						//double startServiceTime=part1.getFuture().getstartServiceTime()+part1.getFuture().getReqTime()+test.getloadTimePatient();
						//present.setStartTime(startServiceTime);
						//present.setEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
						double travelTime= inp.getCarCost().getCost(present.getId()-1, future.getId()-1);
						//double detour=(int) Math.ceil(travelTime*test.getDetour());
						double startServiceTime=present.getStartTime()+travelTime;
						future.setStartTime(startServiceTime);
						future.setSoftStartTime(startServiceTime);
						startServiceTime=present.getEndTime()+travelTime;
						future.setEndTime(startServiceTime);

						future.setSoftEndTime(startServiceTime);
						if(future.getSoftEndTime()<future.getSoftStartTime()) {
							double detour=(int) Math.ceil(travelTime*test.getDetour());
						}
						toInsert.put(future.getSubJobKey(), future);
					}

				}
				//				if(inRoute.isMedicalCentre() && inRoute.getTotalPeople()>0) {// option 2: pick up medical centre
				//					String nextPart="D"+inRoute.getIdUser();
				//
				//					Couple part2=pickpatientMedicalCentre2.get(nextPart);
				//
				//					SubJobs future=new SubJobs(part2.getFuture());
				//					if(!listKJobs.containsKey(future.getSubJobKey())) {
				//						//double startServiceTime=part1.getFuture().getstartServiceTime()+part1.getFuture().getReqTime()+test.getloadTimePatient();
				//						//present.setStartTime(startServiceTime);
				//						//present.setEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
				//						double travelTime= inp.getCarCost().getCost(inRoute.getId()-1, future.getId()-1);
				//						double detour=(int) Math.ceil(travelTime*test.getDetour());
				//						double startServiceTime=inRoute.getStartTime()+travelTime;
				//						future.setStartTime(startServiceTime);
				//						future.setSoftStartTime(startServiceTime);
				//						startServiceTime=inRoute.getEndTime()+travelTime;
				//						future.setEndTime(startServiceTime);
				//						future.setSoftEndTime(startServiceTime);
				//						toInsert.put(future.getSubJobKey(), future);
				//
				//					}
				//				}
				if(inRoute.isClient() && inRoute.getTotalPeople()<0) {// option 3: drop off client home
					Couple part1=dropoffHomeCareStaff2.get(inRoute.getSubJobKey());
					SubJobs present= new SubJobs(part1.getPresent());
					SubJobs future= new SubJobs(part1.getFuture());
					future.setStartTime(inRoute.getendServiceTime());
					future.setEndTime(future.getStartTime()+test.getCumulativeWaitingTime());
					future.setStartServiceTime(future.getEndTime());
					future.setSoftStartTime(inRoute.getendServiceTime());
					future.setSoftEndTime(future.getStartTime()+test.getCumulativeWaitingTime());

					future.setarrivalTime(future.getStartTime());
					future.setdepartureTime(future.getArrivalTime()+test.getloadTimeHomeCareStaff());
					if(!listKJobs.containsKey(future.getSubJobKey())) {
						toInsert.put(future.getSubJobKey(), future);}

				}

			}
		}
	}



	private void generatingNextNodesToInsert(ArrayList<Parts> poolParts, Parts changing, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2,
			HashMap<String, SubJobs> toInsert) {
		HashMap<String, SubJobs> listKJobs= new HashMap<String, SubJobs>();
		// list inserted jobs
		for(Parts p: poolParts) {
			for(SubJobs j:p.getListSubJobs()) {
				listKJobs.put(j.getSubJobKey(), j);
			}
		}

		for(SubJobs inRoute:changing.getListSubJobs()) {

			if(inRoute.isMedicalCentre() && inRoute.getTotalPeople()<0) {// option 1: drop off medical centre
				Couple part1=dropoffpatientMedicalCentre2.get(inRoute.getSubJobKey());

				String nextPart="D"+inRoute.getIdUser();

				Couple part2=pickpatientMedicalCentre2.get(nextPart);
				if(part2==null) {
					//System.out.println("Stop");
				}
				SubJobs present= new SubJobs(part2.getPresent());
				present.setloadUnloadRegistrationTime(0);
				SubJobs future=new SubJobs(part2.getFuture());
				if(!listKJobs.containsKey(present.getSubJobKey())) {
					double startServiceTime=inRoute.getendServiceTime();
					present.setStartTime(startServiceTime);
					present.setEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
					present.setSoftStartTime(startServiceTime);
					present.setSoftEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
					toInsert.put(present.getSubJobKey(), present);
				}
				if(!listKJobs.containsKey(future.getSubJobKey())) {
					//double startServiceTime=part1.getFuture().getstartServiceTime()+part1.getFuture().getReqTime()+test.getloadTimePatient();
					//present.setStartTime(startServiceTime);
					//present.setEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
					double travelTime= inp.getCarCost().getCost(present.getId()-1, future.getId()-1);
					//double detour=(int) Math.ceil(travelTime*test.getDetour());
					double startServiceTime=present.getStartTime()+travelTime;
					future.setStartTime(startServiceTime);
					future.setSoftStartTime(startServiceTime);
					startServiceTime=present.getEndTime()+travelTime;
					future.setEndTime(startServiceTime);

					future.setSoftEndTime(startServiceTime);
					if(future.getSoftEndTime()<future.getSoftStartTime()) {
						double detour=(int) Math.ceil(travelTime*test.getDetour());
					}

				}

			}
			if(inRoute.isMedicalCentre() && inRoute.getTotalPeople()>0) {// option 2: pick up medical centre
				String nextPart="D"+inRoute.getIdUser();

				Couple part2=pickpatientMedicalCentre2.get(nextPart);

				SubJobs future=new SubJobs(part2.getFuture());
				if(!listKJobs.containsKey(future.getSubJobKey())) {
					//double startServiceTime=part1.getFuture().getstartServiceTime()+part1.getFuture().getReqTime()+test.getloadTimePatient();
					//present.setStartTime(startServiceTime);
					//present.setEndTime(present.getStartTime()+test.getCumulativeWaitingTime());
					double travelTime= inp.getCarCost().getCost(inRoute.getId()-1, future.getId()-1);
					double detour=(int) Math.ceil(travelTime*test.getDetour());
					double startServiceTime=inRoute.getStartTime()+travelTime;
					future.setStartTime(startServiceTime);
					future.setSoftStartTime(startServiceTime);
					startServiceTime=inRoute.getEndTime()+travelTime;
					future.setEndTime(startServiceTime);
					future.setSoftEndTime(startServiceTime);
					toInsert.put(future.getSubJobKey(), future);

				}
			}
			if(inRoute.isClient() && inRoute.getTotalPeople()<0) {// option 3: drop off client home
				Couple part1=dropoffHomeCareStaff2.get(inRoute.getSubJobKey());
				SubJobs present= new SubJobs(part1.getPresent());
				SubJobs future= new SubJobs(part1.getFuture());
				future.setStartTime(present.getendServiceTime());
				future.setEndTime(future.getStartTime()+test.getCumulativeWaitingTime());
				future.setStartServiceTime(future.getEndTime());
				future.setSoftStartTime(present.getendServiceTime());
				future.setSoftEndTime(future.getStartTime()+test.getCumulativeWaitingTime());

				future.setarrivalTime(future.getStartTime());
				future.setdepartureTime(future.getArrivalTime()+test.getloadTimeHomeCareStaff());
				if(!listKJobs.containsKey(future.getSubJobKey())) {
					toInsert.put(future.getSubJobKey(), future);}

			}

		}

	}


	private boolean inseringRoute(Jobs j, Parts p) {
		boolean inserted= false;
		if(p.getListSubJobs().isEmpty() ) {
			inserted= true;

			if(j.isPatient()) { // pick up 
				//if(subJobsList.get(0).getTotalPeople()>0) { // pick up 
				j.setStartServiceTime(j.getStartTime());
				j.setarrivalTime(j.getstartServiceTime());
				j.setdepartureTime(j.getArrivalTime()+test.getloadTimePatient());
				j.setEndServiceTime(j.getDepartureTime());

				//}
			}
			if(j.isMedicalCentre() || j.isClient() ) {
				if(j.getTotalPeople()<0) { // pick up 
					j.setStartServiceTime(j.getSoftStartTime());
					j.setarrivalTime(j.getstartServiceTime());
					j.setdepartureTime(j.getArrivalTime()+test.getloadTimePatient());
					j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
				}
				else {
					{ // pick up 
						j.setStartServiceTime(j.getStartTime());
						j.setarrivalTime(j.getstartServiceTime());
						j.setdepartureTime(j.getArrivalTime()+test.getloadTimePatient());
						j.setEndServiceTime(j.getDepartureTime());

					}
				}
			}

			p.getListSubJobs().add((SubJobs)j);


		}
		else { // iterating over Route
			ArrayList<SubJobs> sequence= new ArrayList<SubJobs>();
			for(SubJobs jobsInRoute:p.getListSubJobs()) {
				sequence.add(jobsInRoute);
			}

			sequence.add((SubJobs)j);

			sequence.sort(Jobs.SORT_BY_STARTW);
			inserted=testing(sequence);

			if(inserted) {
				//
				for(int index=1;index<sequence.size();index++) {
					SubJobs a=sequence.get(index-1);
					SubJobs b=sequence.get(index);
					double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
					double calculatedStartTime=0;
					double possibleStartTime=0;
					if(b.isMedicalCentre() ) {
						if(b.getTotalPeople()<0) {
							calculatedStartTime=(a.getDepartureTime()+tv+test.getRegistrationTime());
							possibleStartTime=Math.max(b.getEndTime(), calculatedStartTime);
						}
						else {
							calculatedStartTime=(a.getDepartureTime()+tv);
							possibleStartTime=Math.max(b.getStartTime(), calculatedStartTime);
						}
					}
					else {
						if(b.isClient()) {
							calculatedStartTime=(a.getDepartureTime()+tv+test.getloadTimeHomeCareStaff());
						}
						else {
							calculatedStartTime=(a.getDepartureTime()+tv+test.getloadTimePatient());
						}
						possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);
					}

					b.setStartServiceTime(possibleStartTime);
					b.setarrivalTime(a.getDepartureTime()+tv);

					if(b.isClient()) {
						if(b.getTotalPeople()<0) {
							b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
							b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());}
						else {
							b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
							b.setEndServiceTime(b.getDepartureTime());
						}
					}
					else {
						if(b.getTotalPeople()<0) {
							b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
							b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());}
						else {
							b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
							b.setEndServiceTime(b.getDepartureTime());
						}
					}
				}

				p.getListSubJobs().clear();
				p.getDirectorySubjobs().clear();
				for(SubJobs jobsInRoute:sequence) {
					p.getListSubJobs().add(jobsInRoute);
					p.getDirectorySubjobs().put(jobsInRoute.getSubJobKey(), jobsInRoute);
				}
			}
		}
		return inserted;
	}

	private boolean inseringRoute(Jobs j, Parts p, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff2,boolean hardConstraints) {
		boolean inserted= false;

		ArrayList<SubJobs> subJobsList= listJobAssociatedToJ(j,dropoffpatientMedicalCentre2,dropoffHomeCareStaff2,pickpatientMedicalCentre2,pickupHomeCareStaff2);
		if(p.getListSubJobs().isEmpty() ) {
			inserted= true;
			if(subJobsList.get(0).isPatient()) { // pick up 
				subJobsList.get(0).setStartServiceTime(subJobsList.get(0).getStartTime());
				subJobsList.get(0).setarrivalTime(subJobsList.get(0).getstartServiceTime());
				subJobsList.get(0).setdepartureTime(subJobsList.get(0).getArrivalTime()+test.getloadTimePatient());
				subJobsList.get(0).setEndServiceTime(subJobsList.get(0).getDepartureTime());
			}
			if(subJobsList.get(0).isMedicalCentre() || subJobsList.get(0).isClient() ) {
				if(subJobsList.get(0).getTotalPeople()<0) { // pick up 
					subJobsList.get(0).setStartServiceTime(subJobsList.get(0).getSoftStartTime());
					subJobsList.get(0).setarrivalTime(subJobsList.get(0).getstartServiceTime());
					subJobsList.get(0).setdepartureTime(subJobsList.get(0).getArrivalTime()+test.getloadTimePatient());

					subJobsList.get(0).setEndServiceTime(subJobsList.get(0).getstartServiceTime()+subJobsList.get(0).getReqTime());
				}
				else {
					{ // pick up 
						subJobsList.get(0).setStartServiceTime(subJobsList.get(0).getStartTime());
						subJobsList.get(0).setarrivalTime(subJobsList.get(0).getstartServiceTime());
						subJobsList.get(0).setdepartureTime(subJobsList.get(0).getArrivalTime()+test.getloadTimePatient());
						subJobsList.get(0).setEndServiceTime(subJobsList.get(0).getDepartureTime());

					}
				}
			}

			p.getListSubJobs().add(subJobsList.get(0));
			p.getDirectorySubjobs().put(subJobsList.get(0).getSubJobKey(), subJobsList.get(0));
			for(int index=1;index<subJobsList.size();index++) {
				SubJobs a=subJobsList.get(index-1);
				SubJobs b=subJobsList.get(index);
				double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
				double calculatedStartTime=0;
				double possibleStartTime=0;
				if(b.isMedicalCentre() ) {
					if(b.getTotalPeople()<0) {
						calculatedStartTime=(a.getDepartureTime()+tv+b.getloadUnloadRegistrationTime());
						possibleStartTime=Math.max(b.getSoftEndTime(), calculatedStartTime);
					}
					else {
						calculatedStartTime=(a.getDepartureTime()+tv);
						possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);

					}
				}
				else {
					calculatedStartTime=(a.getDepartureTime()+tv);
					possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);
				}

				b.setStartServiceTime(possibleStartTime);
				b.setarrivalTime(a.getDepartureTime()+tv);
				b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());
				if(b.isClient()) {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
				}
				else {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
				}
				p.getListSubJobs().add(b);
				p.getDirectorySubjobs().put(b.getSubJobKey(), b);
			}


			//updatingTimes(p.getListSubJobs());
		}
		else { // iterating over Route
			double s=0;
			ArrayList<SubJobs> sequence= insertingJob(p,subJobsList,dropoffpatientMedicalCentre2,dropoffHomeCareStaff2,pickpatientMedicalCentre2,pickupHomeCareStaff2, hardConstraints);

			if(!sequence.isEmpty()) {
				inserted=false;
				boolean capTW=testing(sequence); // tw and cap
				boolean deourFeasible=checkingInsertionRespectoToDetour(sequence,subJobsList);		// detour
				if(capTW && deourFeasible) {
					inserted=true;
				}

				if(inserted) {

					SettingTimesSequence(sequence);

					//


					p.getListSubJobs().clear();
					p.getDirectorySubjobs().clear();
					for(SubJobs jobsInRoute:sequence) {
						p.getListSubJobs().add(jobsInRoute);
						p.getDirectorySubjobs().put(jobsInRoute.getSubJobKey(), jobsInRoute);
					}
				}
			}			
		}
		//	checkingWaitingTimes(p);

		return inserted;
	}

	private boolean checkingInsertionRespectoToDetour(ArrayList<SubJobs> sequence, ArrayList<SubJobs> subJobsList2) {
		boolean deourFeasible=true;
		if(subJobsList2.size()>=2) { // patient 
			Edge e= new Edge (subJobsList2.get(0),subJobsList2.get(1),inp,test);
			double distance=0;

			boolean count=false;
			for(int jindex=1;jindex<sequence.size();jindex++) {
				SubJobs j=sequence.get(jindex-1);
				SubJobs k=sequence.get(jindex);
				if(j.getSubJobKey().equals("D5")) {
					//System.out.println("Sol");
				}
				if(j.getSubJobKey().equals(e.getOrigin().getSubJobKey())) {
					if(j.getSubJobKey().equals("D61")) {
						//System.out.println("Sol");
					}
					count=true;
					distance=0;
				}
				if(count) {
					distance+=inp.getCarCost().getCost(j.getId()-1, k.getId()-1);
				}
				if(distance>10) {
					//System.out.println("Sol");	
				}
				if(k.getSubJobKey().equals(e.getEnd().getSubJobKey()) && count) {
					break;
				}
			}

			if(distance>e.getDetour()) {
				deourFeasible=false;
			}
		}
		return deourFeasible;
	}




	private void updatingTime(SubJobs last, ArrayList<SubJobs> copy) {
		//		if(last.isPatient()) {// patient
		//			if(last.getTotalPeople()<0) { // buscar el nodo pick Up
		//				for(SubJobs j: copy) {
		//					if(j.getId()==last.getsubJobPair().getId()) { // se va a cambiar la hora de j
		//						double tv=inp.getCarCost().getCost(j.getId()-1, last.getId()-1);
		//						
		//						break;
		//					}
		//			}
		//		}
		//		else {
		if(last.isMedicalCentre()) {// medical centre
			if(last.getTotalPeople()<0) { 
				for(SubJobs j: copy) {
					if(j.getId()==last.getIdUser()) { // se va a cambiar la hora de j
						double tv=inp.getCarCost().getCost(j.getId()-1, last.getId()-1);
						double departure=last.getArrivalTime()-tv;
						double endTW=departure-computingLeavlingNode(j); // ideal arrival
						double startTW=endTW-test.getCumulativeWaitingTime(); // ideal arrival
						j.setStartTime(startTW);
						j.setEndTime(endTW);
						break;
					}
				}	
			}
		}



	}

	private double computingLeavlingNode(SubJobs a) {
		double additionalTime=0;
		if(a.isClient()) {
			additionalTime=test.getloadTimeHomeCareStaff();
		}
		else {
			additionalTime=test.getloadTimePatient();
		}

		return additionalTime;
	}

	private double maxValue(double[] waitingTimes) {
		double max=Double.MIN_VALUE;
		for(int i=0;i<waitingTimes.length;i++) {
			if(test.getCumulativeWaitingTime()<waitingTimes[i]) {
				max=waitingTimes[i];
			}

		}
		return max;
	}

	private double determineAdditionalTime(SubJobs j) {
		double additionalTime=0;
		if(j.getTotalPeople()<0) {
			additionalTime=timeDropOffBeforeService(j); //medical centre, patient, client
		}
		else {
			//additionalTime=timePickUpAfterService(j);
		}
		return additionalTime;
	}

	private double timePickUpAfterService(SubJobs j) {
		double additionalTime=0;
		if(j.isMedicalCentre()) {// medical centre
			additionalTime=test.getloadTimePatient();
		}
		else {
			if(j.isClient()) {// client
				additionalTime=test.getloadTimeHomeCareStaff();
			}
			else {//patient
				additionalTime=test.getloadTimePatient();

			}
		}
		return additionalTime;
	}

	private double timeDropOffBeforeService(SubJobs j) {
		double additionalTime=0;
		//medical centre, patient, client
		if(j.isMedicalCentre()) {// medical centre
			//additionalTime=test.getRegistrationTime();
			additionalTime=test.getloadTimePatient()+test.getRegistrationTime();
		}
		else {
			if(j.isClient()) {// client
				additionalTime=test.getloadTimeHomeCareStaff();
			}
			else {//patient
				//	additionalTime=test.getloadTimePatient();

			}
		}
		return additionalTime;
	}

	private void SettingTimesSequence(ArrayList<SubJobs> sequence) {

		for(int index=1;index<sequence.size();index++) {
			SubJobs a=sequence.get(index-1);
			SubJobs b=sequence.get(index);
			double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
			double calculatedStartTime=0;
			double possibleStartTime=0;

			if(b.isMedicalCentre() ) {
				if(b.getTotalPeople()<0) {
					calculatedStartTime=(a.getDepartureTime()+tv+test.getRegistrationTime());
					possibleStartTime=Math.max(b.getSoftEndTime(), calculatedStartTime);
				}
				else {
					calculatedStartTime=(a.getDepartureTime()+tv);
					possibleStartTime=Math.max(b.getStartTime(), calculatedStartTime);
				}
			}
			else {
				if(b.isClient()) {
					calculatedStartTime=(a.getDepartureTime()+tv+test.getloadTimeHomeCareStaff());
				}
				else {
					calculatedStartTime=(a.getDepartureTime()+tv+test.getloadTimePatient());
				}
				possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);
			}

			b.setStartServiceTime(possibleStartTime);
			b.setarrivalTime(a.getDepartureTime()+tv);

			if(b.isClient()) {
				if(b.getTotalPeople()<0) {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
					b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());}
				else {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
					b.setEndServiceTime(b.getDepartureTime());
				}
			}
			else {
				if(b.getTotalPeople()<0) {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
					b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());}
				else {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
					b.setEndServiceTime(b.getDepartureTime());
				}
			}
		}
		for(SubJobs j:sequence) {
			double additionaltime=determineAdditionalTime(j);

			double delta=(j.getstartServiceTime()-(j.getArrivalTime()+additionaltime));
			if(delta>test.getCumulativeWaitingTime()) {
				//System.out.println(j.toString());
			}
		}
	}

	private boolean testing(ArrayList<SubJobs> sequence) {
		boolean feasible = false;
		if(vehicleCapacityPart(sequence)) {

			double departureTime=sequence.get(0).getDepartureTime();
			for(int index=1;index<sequence.size();index++) {
				feasible=false;
				SubJobs a=sequence.get(index-1);
				SubJobs b=sequence.get(index);

				if(b.getSubJobKey().equals("D44")) {
					//System.out.println(b.toString());
				}

				double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);			
				double calculatedStartTime=0;
				double possibleStartTime=0;

				// valores definitivos
				double startServiceTime=0;
				double arrivalTime=0;
				double additionalTime=0;

				if(b.isMedicalCentre() ) {
					if(b.getTotalPeople()<0) {
						calculatedStartTime=(departureTime+tv+test.getRegistrationTime());
						possibleStartTime=Math.max(b.getSoftEndTime(), calculatedStartTime);
						additionalTime=test.getRegistrationTime();
					}
					else {
						calculatedStartTime=(departureTime+tv);
						possibleStartTime=Math.max(b.getStartTime(), calculatedStartTime);
					}
				}
				else {
					if(b.isClient()) {
						if(b.getTotalPeople()<0) {// drop off
							calculatedStartTime=(departureTime+tv+test.getloadTimeHomeCareStaff());
							additionalTime=test.getloadTimeHomeCareStaff();
						}
						else { // pick up
							calculatedStartTime=(departureTime+tv);
						}
					}
					else {
						calculatedStartTime=(departureTime+tv+test.getloadTimePatient());
						additionalTime=test.getloadTimePatient();
					}
					possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);
				}

				startServiceTime=possibleStartTime;
				arrivalTime=departureTime+tv;
				additionalTime=determineAdditionalTime(b);
				if(startServiceTime-(arrivalTime+additionalTime)<=test.getCumulativeWaitingTime()) {
					if(b.isClient()) {
						if(b.getTotalPeople()<0) {
							departureTime=arrivalTime+test.getloadTimeHomeCareStaff();
						}
						else {
							departureTime=arrivalTime+test.getloadTimeHomeCareStaff();
						}
					}
					else {
						if(b.getTotalPeople()<0) {
							departureTime=arrivalTime+test.getloadTimePatient();
						}
						else {
							departureTime=arrivalTime+test.getloadTimePatient();
						}
					}

					// validation TW
					if(b.isMedicalCentre() || b.isPatient()) {
						boolean logic=sequenceLogic(a,b);
						if(arrivalTime>=(b.getStartTime()) && arrivalTime<=b.getEndTime() && logic) {
							feasible=true;
						}
						if(!feasible) {
							break;	
						}
					}
					if(b.isClient() ) {
						boolean logic=sequenceLogicClient(a,b);
						if(arrivalTime>=(b.getSoftStartTime()) && startServiceTime<=b.getSoftEndTime() && logic) {
							feasible=true;
						}
						if(!feasible) {
							break;	
						}

					}
				}
				else {
					break;
				}
			}
		}

		// detour <- con respecto al depot

		return feasible;
	}


	private boolean sequenceLogicClient(SubJobs a, SubJobs b) {
		boolean feasible=true;
		if(a.getId()==b.getId() && a.isClient() && b.isClient()) {
			if(a.getTotalPeople()>0 && b.getTotalPeople()<0) {
				feasible=false;
			}}
		else {
			if(a.getTotalPeople()>0 && b.getTotalPeople()<0) {
				if(a.getId()==b.getsubJobPair().getId()) {
					feasible=false;
				}	
			}
		}
		return feasible;
	}

	private boolean sequenceLogic(SubJobs a, SubJobs b) {
		boolean feasible =true;
		if(b.isMedicalCentre()) {
			if(a.getId()==b.getIdUser()) {
				if(a.getTotalPeople()<0 && b.getTotalPeople()>0) {
					feasible =false;
				}
				//				if(a.getTotalPeople()>0 && b.getTotalPeople()<0) {
				//					feasible =false;
				//				}
			}
		}
		if(b.isPatient()) {
			if(a.getIdUser()==b.getId()) {
				if(a.getTotalPeople()<0 && b.getTotalPeople()>0) {
					feasible =false;
				}
				if(a.getTotalPeople()>0 && b.getTotalPeople()<0) {
					feasible =false;
				}

			}
		}
		return feasible;
	}

	private ArrayList<SubJobs> insertingJob(Parts p, ArrayList<SubJobs> subJobsList2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff2, boolean hardConstraints) {

		ArrayList<SubJobs> hardConstraintsJobs = new ArrayList<SubJobs>();
		ArrayList<SubJobs> estimatedTimes= new ArrayList<SubJobs>();
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();

		if(hardConstraints) {
			hardConstraintsJobs = jobsHardTimeWindow(p,subJobsList2); // selection of jobs con hard time window
			list = integratingJobs(hardConstraintsJobs); // se insertan
			estimatedTimes= jobsEstimatedTimeWindow(subJobsList2,p,dropoffpatientMedicalCentre2,dropoffHomeCareStaff2,pickpatientMedicalCentre2,pickupHomeCareStaff2);
			estimatedTimes.sort(Jobs.SORT_BY_STARTW);
		}
		else {
			for(SubJobs j:subJobsList2 ) {
				estimatedTimes.add(j);
			}
			for(SubJobs j:p.getListSubJobs()) {
				list.add(j);
			}
			estimatedTimes.sort(Jobs.SORT_BY_STARTW);
		}
		if(!estimatedTimes.isEmpty() && !list.isEmpty() && hardConstraints) {

			for(SubJobs r: estimatedTimes) {
				boolean inserted=false;
				for(int i=0;i<list.size();i++) { //	r --- j
					SubJobs j=list.get(i);
					ArrayList<SubJobs> copy= new ArrayList<SubJobs>();

					for(int aux=0;aux<i;aux++) {
						copy.add(list.get(aux));
					}
					copy.add(r);
					copy.add(list.get(i));
					inserted= testing(copy);
					if(inserted) {
						int index=list.indexOf(j);
						list.add(index,r);
						inserted=true;
						break;
					}
				}
				if(!inserted) {	//  j --- r
					ArrayList<SubJobs> copy= new ArrayList<SubJobs>();

					for(int aux=0;aux<list.size();aux++) {
						copy.add(list.get(aux));
					}
					copy.add(r);

					inserted= testing(copy);
					if(inserted) {					
						list.add(r);
						inserted=true;
					}	}
				if(!inserted) {
					list.clear();
					break;
				}
			}
		}
		if(!hardConstraints) {
			// insertar 1 parte
			if(estimatedTimes.get(0).getSubJobKey().equals("P28") && p.getDirectorySubjobs().containsKey("P55")) {
				//System.out.println(estimatedTimes.get(0).toString());
			}
			boolean partI=feasibleInsertion(estimatedTimes,list);

			System.out.print("System out print");
			System.out.print(p.toString());
			if(!partI) {
				list.clear();
			}

		}
		else {
			if(!estimatedTimes.isEmpty() && hardConstraintsJobs.isEmpty() && hardConstraints) {
				boolean inserted=testing(estimatedTimes);
				if(inserted) {
					SettingTimesSequence(estimatedTimes);
					for(SubJobs j:estimatedTimes) {
						list.add(j);
					}
				}
			}

		}
		return list;
	}

	private boolean feasibleInsertion(ArrayList<SubJobs> estimatedTimes, ArrayList<SubJobs> list) {
		boolean feasible=false;
		ArrayList<SubJobs> newSequence= new ArrayList<SubJobs>();
		ArrayList<SubJobs> listCopy= new ArrayList<SubJobs>();
		for(SubJobs j:list) {
			listCopy.add(j);
		}

		for(SubJobs toInsert:estimatedTimes) {
			feasible=false;
			for(SubJobs inserted:listCopy) {
				if(listCopy.indexOf(inserted)==0) {
					feasible=firstJobSequence(inserted,toInsert);
				}
				else {
					if(listCopy.indexOf(inserted)==listCopy.size()-1) {
						feasible=lastJobSequence(inserted,toInsert);
						if(feasible) {
							if(!newSequence.contains(inserted)) {
							newSequence.add(inserted);}
							if(!newSequence.contains(toInsert)) {
							newSequence.add(toInsert);}
						}
					}
					else {
						//(i-1)(position to insert) (i)
						int indexPrevious=listCopy.indexOf(inserted)-1;
						SubJobs previous=listCopy.get(indexPrevious);
						feasible=intermediateJobSequence(previous,toInsert,inserted);
					}
				}
				if(feasible && listCopy.indexOf(inserted)!=listCopy.size()-1) {
					if(!newSequence.contains(toInsert)) {
						newSequence.add(toInsert);}
					if(!newSequence.contains(inserted)) {
					newSequence.add(inserted);}
				}
				else {
					if(listCopy.indexOf(inserted)!=listCopy.size()-1) {
						if(!newSequence.contains(inserted)) {
							newSequence.add(inserted);}
					}
				}
			}	
			if(!feasible) {
				break;
			}
		}
		if(feasible) {
			list.clear();
			for(SubJobs j:newSequence) {
				list.add(j);
			}
		}

		return feasible;
	}

	private boolean intermediateJobSequence(SubJobs previous, SubJobs toInsert, SubJobs inserted) {
		boolean feasible =false;
		if((toInsert.getStartTime()<=inserted.getArrivalTime() && previous.getDepartureTime()>= toInsert.getStartTime()) 
				|| (toInsert.getEndTime()<=inserted.getArrivalTime() && previous.getDepartureTime()>= toInsert.getEndTime())) {
			feasible =true;
		}
		return feasible;
	}

	private boolean lastJobSequence(SubJobs inserted, SubJobs toInsert) {
		boolean feasible= false;
		double tv= inp.getCarCost().getCost(inserted.getId()-1, toInsert.getId()-1);
		double arrrival=inserted.getDepartureTime()+tv;
		if(arrrival>toInsert.getStartTime() && arrrival<toInsert.getEndTime()) {
			feasible= true;
		}

		return feasible;
	}

	private boolean firstJobSequence(SubJobs inserted, SubJobs toInsert) {
		// (inserted)ArrayList<SubJobs> toInsert
		boolean feasible= false;
		double tv=inp.getCarCost().getCost(toInsert.getId()-1, inserted.getId()-1);
		double departure=inserted.getArrivalTime()-tv;
		double arrival=departure-toInsert.getloadUnloadTime()-toInsert.getloadUnloadRegistrationTime();
		if(arrival>toInsert.getStartTime() && arrival<toInsert.getEndTime()) {
			feasible= true;
		}
		else {
			departure=toInsert.getStartTime()+tv;
			if(departure<inserted.getArrivalTime()) {
				feasible= true;
			}	
		}
		return feasible;
	}

	private ArrayList<SubJobs> jobsHardTimeWindow(Parts p, ArrayList<SubJobs> subJobsList2) {
		ArrayList<SubJobs>  list= new ArrayList<SubJobs>();
		for(SubJobs j: p.getListSubJobs()) {
			if(j.getSubJobKey().equals("D5167")) {
				//System.out.println("Stop");
			}
			if(j.getTotalPeople()<0) {
				if(j.isMedicalCentre() || j.isClient()) {
					list.add(j);
				}	
			}
		}
		for(SubJobs j: subJobsList2) {
			if(j.getTotalPeople()<0) {
				if(j.isMedicalCentre() || j.isClient()) {
					list.add(j);
				}	
			}
		}
		return list;
	}

	private ArrayList<SubJobs> jobsEstimatedTimeWindow(ArrayList<SubJobs> jobslist,
			Parts p , HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff2) {
		ArrayList<SubJobs> allTransportRequest= new ArrayList<SubJobs>(); 
		for(SubJobs j: p.getListSubJobs() ) {

			if(j.getSubJobKey().equals("P5167")) {
				//System.out.println("Solu");
			}
			if(j.isPatient()) {
				allTransportRequest.add(j);
			}
			if(j.isMedicalCentre() && j.getTotalPeople()>0) {
				allTransportRequest.add(j);
			}
			if(j.isClient() && j.getTotalPeople()>0) {
				allTransportRequest.add(j);
			}
		}
		for(SubJobs j: jobslist ) {
			if(j.isPatient()) {
				allTransportRequest.add(j);
			}
			if(j.isMedicalCentre() && j.getTotalPeople()>0) {
				allTransportRequest.add(j);
			}
			if(j.isClient() && j.getTotalPeople()>0) {
				allTransportRequest.add(j);
			}
		}

		return allTransportRequest;
	}

	private ArrayList<SubJobs> integratingJobs(ArrayList<SubJobs> hardConstraintsJobs) {
		ArrayList<SubJobs> list = new ArrayList<SubJobs>();
		for(SubJobs j: hardConstraintsJobs) {
			list.add(j);
		}
		list.sort(Jobs.SORT_BY_STARTW);

		return list;
	}



	private ArrayList<SubJobs> listJobAssociatedToJ(Jobs j, HashMap<String, Couple> dropoffpatientMedicalCentre2,
			HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff2) {
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		if(j.getSubJobKey().equals("D5167")) {
			//System.out.println("Stop");
		}
		if(j.isMedicalCentre() && j.getTotalPeople()<0) {  // drop- off
			Couple c=dropoffpatientMedicalCentre2.get(j.getSubJobKey());
			SubJobs present=new SubJobs(c.getPresent());
			SubJobs future=new SubJobs(j);
			future.setEndTime(future.getSoftEndTime()-(future.getloadUnloadRegistrationTime()+future.getloadUnloadTime()));
			future.setStartTime(future.getEndTime()-test.getCumulativeWaitingTime());
			// setting start service time
			double travelTime=inp.getCarCost().getCost(present.getId()-1, future.getId()-1);
			present.setStartTime(future.getStartTime()-travelTime);
			present.setEndTime(future.getEndTime()-travelTime);
			future.setStartServiceTime(future.getSoftEndTime());
			future.setarrivalTime(future.getstartServiceTime()-(future.getloadUnloadRegistrationTime()+future.getloadUnloadTime()));
			future.setEndServiceTime(future.getstartServiceTime()+future.getReqTime());
			present.setdepartureTime(present.getEndTime());
			//present.setStartServiceTime(future.getArrivalTime()-travelTime);
			present.setarrivalTime(present.getDepartureTime()-present.getloadUnloadTime());
			future.setdepartureTime(future.getArrivalTime()+future.getloadUnloadTime());


			present.setStartServiceTime(present.getArrivalTime());
			present.setEndServiceTime(present.getstartServiceTime());
			list.add(present);
			list.add(future);
		}
		if(j.isMedicalCentre() && j.getTotalPeople()>0) {  // pick Up
			String key="D"+j.getIdUser();
			Couple c=pickpatientMedicalCentre2.get(key);
			SubJobs present=new SubJobs(j);
			SubJobs future=new SubJobs(c.getFuture());
			// setting start service time
			double travelTime=inp.getCarCost().getCost(present.getId()-1, future.getId()-1);
			future.setStartTime(present.getStartTime()+travelTime);
			future.setEndTime(present.getEndTime()+travelTime);
			future.setSoftStartTime(future.getStartTime());
			future.setSoftEndTime(future.getEndTime());
			future.setStartServiceTime(future.getEndTime());
			//future.setarrivalTime(future.getstartServiceTime()-(future.getloadUnloadRegistrationTime()+future.getloadUnloadTime()));
			future.setarrivalTime(future.getEndTime());
			future.setEndServiceTime(future.getstartServiceTime()+future.getReqTime());
			future.setdepartureTime(future.getArrivalTime()+future.getloadUnloadTime());
			list.add(present);
			list.add(future);

			if(future.getSoftEndTime()<future.getSoftStartTime()) {
				double detour=(int) Math.ceil(travelTime*test.getDetour());
			}
		}
		if(j.isClient() ) {
			j.setarrivalTime(j.getStartTime());
			j.setdepartureTime(j.getArrivalTime()+test.getloadTimeHomeCareStaff());
			list.add((SubJobs) j);
			//			if(j.getTotalPeople()<0) { // drop-off
			//				Couple c=dropoffHomeCareStaff2.get(j.getSubJobKey());
			//				SubJobs present=(SubJobs)c.getPresent();
			//				//present.setClient(true);
			//				//c.getFuture().setClient(true);
			//				list.add(present);
			//			}
			//			else {  // pick up
			//				Couple c=pickupHomeCareStaff2.get(j.getSubJobKey());
			//				SubJobs future=(SubJobs)c.getFuture();
			//				//future.setClient(true);
			//				//c.getPresent().setClient(true);
			//				list.add(future);
			//			}
		}
		if(j.isPatient() && j.getTotalPeople()<0) {
			Couple c=pickpatientMedicalCentre2.get(j.getSubJobKey());
			SubJobs present=(SubJobs)c.getPresent();
			SubJobs future=(SubJobs)c.getFuture();
			//present.setMedicalCentre(true);
			//	present.setPatient(false);
			//future.setPatient(true);
			//future.setMedicalCentre(false);
			list.add(present);
			list.add(future);
			if(future.getSubJobKey().equals("D57")) {
				if(future.isMedicalCentre()) {
					//System.out.println("Stop");
				}
			}
			if(present.getSubJobKey().equals("D57")) {
				if(present.isMedicalCentre()) {
					//System.out.println("Stop");
				}
			}
		}
		return list;
	}






	private ArrayList<Parts> selectionBestParst(Jobs j,ArrayList<Parts> poolParts) {
		ArrayList<Parts> bestRoutes= new ArrayList<Parts>();
		double departure=0;
		for(Parts p:poolParts) {
			if(!p.getListSubJobs().isEmpty()) {
				departure=p.getListSubJobs().get(p.getListSubJobs().size()-1).getDepartureTime();
				if(departure<=j.getArrivalTime()) {
					bestRoutes.add(p);
				}}
			else {
				bestRoutes.add(p);	
			}
		}
		computingFactorToSortRoutes(j,bestRoutes);
		bestRoutes.sort(Parts.SORT_BY_RouteDistanceNode);
		return bestRoutes;
	}

	private void computingFactorToSortRoutes(Jobs j, ArrayList<Parts> bestRoutes) {

		for(Parts p:bestRoutes) {
			if(!p.getListSubJobs().isEmpty()) {
				SubJobs lastNode=p.getListSubJobs().get(p.getListSubJobs().size()-1);
				double tv=inp.getCarCost().getCost(lastNode.getId()-1, j.getId()-1);
				p.setAdditionalCriterion(tv);
			}
			else {
				p.setAdditionalCriterion(1000);
			}
		}

	}

	private ArrayList<Parts> poolPartscreationRoute() {
		ArrayList<Parts> poolParts= new ArrayList<Parts>();
		for(int i=0;i<inp.getVehicles().get(0).getQuantity();i++) {
			Parts p= new Parts();
			poolParts.add(p);
		}
		return poolParts;
	}

	private ArrayList<Jobs> sortingJobsList(HashMap<String, Couple> dropoffHomeCareStaff, HashMap<String, Couple> dropoffpatientMedicalCentre) {
		ArrayList<Jobs> sort= new ArrayList<Jobs>();
		for(Couple c:dropoffHomeCareStaff.values()) { // dropoffHomeCareStaff <- present
			Jobs j = c.getPresent();
			double size=Math.max(1, j.getStartTime())*(j.getEndTime()-j.getStartTime());
			j.setsortETWSizeCriterion(size);
			size=j.getEndTime()*(j.getEndTime()-j.getStartTime());
			j.setsortLTWSizeCriterion(size);
			sort.add(j);
		}

		for(Couple c: dropoffpatientMedicalCentre.values()) { // dropoffpatientMedicalCentre <- future
			Jobs j = c.getFuture();
			double size=Math.max(1, j.getStartTime())*(j.getEndTime()-j.getStartTime());
			j.setsortETWSizeCriterion(size);
			size=j.getEndTime()*(j.getEndTime()-j.getStartTime());
			j.setsortLTWSizeCriterion(size);
			sort.add(j);
		}
		//sort.sort(Jobs.SORT_BY_STARTW);
		int sortCriterion=rn.nextInt(4)+1;// 1 distance depot // 2 early TW // 3 size 
		switch(sortCriterion){
		case 1:  
			computingAdditionalCriteria(sort, 1);
			sort.sort(Jobs.SORT_DistaceLastConnectedNode);
			break;
		case 2:  sort.sort(Jobs.SORT_BY_STARTW);
		break;
		case 3:  sort.sort(Jobs.TWSIZE_Early);
		break;
		case 4:  Collections.shuffle(sort,rn);
		break;
		}

		return sort;
	}

	private ArrayList<SubJobs> sortingJobsList(ArrayList<SubJobs> dropoffpatientMedicalCentre) {
		ArrayList<SubJobs> sort= new ArrayList<SubJobs>();
		for(SubJobs j: dropoffpatientMedicalCentre) { // dropoffHomeCareStaff <- present
			if(j.isMedicalCentre() && j.getTotalPeople()<0) {
				sort.add(j);
			}
			if(j.isClient() && j.getTotalPeople()<0) {
				sort.add(j);
			}
		}

		//sort.sort(Jobs.SORT_BY_STARTW);
		int sortCriterion=rn.nextInt(3)+1;// 1 distance depot // 2 early TW // 3 size 
		switch(sortCriterion){
		case 1:  sort.sort(Jobs.SORT_BY_STARTW);
		break;
		case 2:  sort.sort(Jobs.TWSIZE_Early);
		break;
		case 3:  Collections.shuffle(sort,rn);
		break;
		}

		return sort;
	}

	private void computingAdditionalCriteria(ArrayList<Jobs> sort, int idLastConnectedNode) {
		// sort according to the last connected node
		for(Jobs sj: sort) {
			double tv=inp.getCarCost().getCost(idLastConnectedNode-1, sj.getId()-1);
			sj.setAdditionalCriterion(tv);
		}

	}

	private HashMap<String, Couple> selectingCouplepickpatientMedicalCentre(HashMap<String, Couple> dropoffpatientMedicalCentre2) {
		HashMap<String, Couple> pickpatientMedicalCentre= new HashMap<>();// soft time windows list of patient
		for(Couple c:this.pickpatientMedicalCentre.values()) {
			String key="D"+c.getPresent().getId()+c.getPresent().getIdUser();
			Couple origin=dropoffpatientMedicalCentre2.get(key);

			SubJobs startNode=new SubJobs(c.getPresent());
			startNode.setMedicalCentre(true);
			startNode.setPatient(false);
			SubJobs endNode=new SubJobs(c.getFuture());
			endNode.setPatient(true);
			endNode.setMedicalCentre(false);
			if(endNode.getSubJobKey().equals("D61")) {
				System.out.print("D61");
			}
			if(startNode.getSubJobKey().equals("D57")) {
				if(startNode.isMedicalCentre()) {
					//System.out.println(startNode.toString());
				}
			}
			if(endNode.getSubJobKey().equals("D57")) {
				if(endNode.isMedicalCentre()) {
					//System.out.println(endNode.toString());
				}
			}
			Couple paar=new Couple(startNode,endNode);
			// travel time
			//			double travelTime=inp.getCarCost().getCost(startNode.getId()-1, endNode.getId()-1);
			//			double detour=(int) Math.ceil(travelTime*test.getDetour());
			//			// adjusting time windows
			//			startNode.setStartTime(origin.getFuture().getStartTime()+origin.getFuture().getReqTime()+test.getloadTimePatient());
			//			startNode.setEndTime(startNode.getStartTime()+test.getCumulativeWaitingTime());
			//			endNode.setStartTime(startNode.getStartTime()+travelTime+test.getloadTimePatient());
			//			endNode.setEndTime(startNode.getEndTime()+detour+test.getloadTimePatient());
			pickpatientMedicalCentre.put(endNode.getSubJobKey(), paar);
		}
		return pickpatientMedicalCentre;
	}

	private HashMap<String, Couple> selectingCoupleDropOffMedical() {
		HashMap<String, Couple> dropoffpatientMedicalCentre= new HashMap<>();// hard time windows list of patient
		for(Couple c:this.dropoffpatientMedicalCentre.values()) {
			SubJobs startNode=new SubJobs(c.getPresent());
			startNode.setPatient(true);
			startNode.setMedicalCentre(false);
			SubJobs endNode=new SubJobs(c.getFuture());
			endNode.setMedicalCentre(true);
			Couple paar=new Couple(startNode,endNode);

			// adjusting time windows
			//			endNode.setStartTime(Math.max(6*60+0.0001,endNode.getArrivalTime()-test.getCumulativeWaitingTime()));
			//			double travelTime=inp.getCarCost().getCost(startNode.getId()-1, endNode.getId()-1);
			//			double detour=(int) Math.ceil(travelTime*test.getDetour());
			//startNode.setStartTime(Math.max(0,endNode.getStartTime()-(test.getloadTimePatient()+detour)));

			//startNode.setEndTime(Math.max(0,endNode.getEndTime()-(test.getloadTimePatient()+travelTime)));
			//endNode.setStartTime(endNode.getArrivalTime());

			//endNode.setEndTime(endNode.getArrivalTime());
			dropoffpatientMedicalCentre.put(endNode.getSubJobKey(), paar);
		}
		return dropoffpatientMedicalCentre;
	}


	private HashMap<String, Couple> selectingCoupleDropOffMedical(Solution diversifiedSolneighborhood) {
		System.out.print(diversifiedSolneighborhood);
		HashMap<String, Couple> dropoffpatientMedicalCentre= new HashMap<>();// hard time windows list of patient
		for(Couple c:this.dropoffpatientMedicalCentre.values()) {
if(c.getFuture().getSubJobKey().equals("D5174")) {
	System.out.println(c.toString());
}
			SubJobs j=new SubJobs(c.getPresent());
			SubJobs jx=new SubJobs(c.getFuture());
			if(jx==null) {
				//System.out.println("j");
			}
			if(j==null) {
				//System.out.println("j");
			}
			SubJobs present=selectingNode(j,diversifiedSolneighborhood); // select node from current solution
			SubJobs endNode=selectingNode(jx,diversifiedSolneighborhood);

			if(endNode.getIdUser()==0) {
				//System.out.println("Stop");
			}
			if(present.getIdUser()==0) {
				//System.out.println("Stop");
			}
			Couple paar=new Couple(present,endNode);

			dropoffpatientMedicalCentre.put(endNode.getSubJobKey(), paar);
		}
		return dropoffpatientMedicalCentre;
	}

	private HashMap<String, Couple> selectingHomeCareStaffCouple(Solution diversifiedSolneighborhood) {
		HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff 
		for(Couple c:this.dropoffHomeCareStaff.values()) {
			SubJobs j=new SubJobs(c.getPresent());
			SubJobs jx=new SubJobs(c.getFuture());
			SubJobs startNode=selectingNode(j,diversifiedSolneighborhood);
			SubJobs endNode=selectingNode(jx,diversifiedSolneighborhood);
			if(endNode.getSubJobKey().equals("D24")) {
				//System.out.println(endNode.toString());
			}
			if(startNode.getSubJobKey().equals("D24")) {
				//System.out.println(startNode.toString());
			}
			Couple paar=new Couple(startNode,endNode);
			dropoffHomeCareStaff.put(startNode.getSubJobKey(), paar);
		}
		return dropoffHomeCareStaff;
	}
	private SubJobs selectingNode(SubJobs present, Solution diversifiedSolneighborhood) {
		SubJobs j=null;
		ArrayList<Jobs> missingJobs= checkingMissingJobs(diversifiedSolneighborhood);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}

		Route r=selectionRoute(present,diversifiedSolneighborhood);
		//System.out.println("Solution" +diversifiedSolneighborhood.toString());
		if(r==null) {
			String key="";
		}
		//for(Route r:diversifiedSolneighborhood.getRoutes()) {
		for(Parts p:r.getPartsRoute()) {
			for(SubJobs sb:p.getListSubJobs()) {
				if(sb.getSubJobKey().equals(present.getSubJobKey())) {
					j=sb;
					break;
				}
			}
			if(j!=null) {
				break;
			}
		}

		return j;
	}

	private Route selectionRoute(SubJobs present, Solution diversifiedSolneighborhood) {
		Route r=null;
		String key="";
		if(present==null){
			key="P60";
		}
		else {
			key=present.getSubJobKey();
		}
		for(Route routeInRoute:diversifiedSolneighborhood.getRoutes()) {
			HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();

			for(Parts p:routeInRoute.getPartsRoute()) {

				for(SubJobs j:p.getListSubJobs()) {
					subJobsList.put(j.getSubJobKey(), j);
				}
				if(subJobsList.containsKey(key)) {
					r=routeInRoute;
					break;
				}
			}
		}
		return r;
	}

	private Route selectionRoute1(SubJobs present, Solution diversifiedSolneighborhood) {
		Route r=null;
		String key="";
		if(present==null){
			key="D4860";
		}
		else {
			key=present.getSubJobKey();
		}
		for(Route routeInRoute:diversifiedSolneighborhood.getRoutes()) {
			HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();

			for(Parts p:routeInRoute.getPartsRoute()) {

				for(SubJobs j:p.getListSubJobs()) {
					subJobsList.put(j.getSubJobKey(), j);
				}
				if(subJobsList.containsKey(key)) {
					r=routeInRoute;
					break;
				}
			}
		}
		return r;
	}

	private Route selectionRoute(SubJobs present, ArrayList<Route> diversifiedSolneighborhood) {
		Route r=null;

		for(Route routeInRoute:diversifiedSolneighborhood) {
			HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();

			for(Parts p:routeInRoute.getPartsRoute()) {

				for(SubJobs j:p.getListSubJobs()) {
					subJobsList.put(j.getSubJobKey(), j);
				}
				if(subJobsList.containsKey(present.getSubJobKey())) {
					r=routeInRoute;
					break;
				}
			}
		}
		return r;
	}



	private HashMap<String, Couple> selectingHomeCareStaffCouple() {
		HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff 
		for(Couple c:this.dropoffHomeCareStaff.values()) {
			SubJobs startNode=new SubJobs(c.getPresent());
			SubJobs endNode=new SubJobs(c.getFuture());
			Couple paar=new Couple(startNode,endNode);

			// adjusting time windows
			startNode.setSoftStartTime(inp.getNodes().get(startNode.getId()-1).getStartTime()); // saving original times
			startNode.setSoftEndTime(inp.getNodes().get(startNode.getId()-1).getEndTime());
			startNode.setStartTime(Math.max(startNode.getStartTime()-test.getCumulativeWaitingTime(), 0)); // including start service time
			endNode.setStartTime(startNode.getStartTime()+startNode.getReqTime()+test.getCumulativeWaitingTime());
			endNode.setEndTime(startNode.getEndTime()+startNode.getReqTime()+test.getCumulativeWaitingTime());
			dropoffHomeCareStaff.put(startNode.getSubJobKey(), paar);
		}
		return dropoffHomeCareStaff;
	}


	private void settingStartEndServiceTime(Route r) {
		for(int p=1;p<r.getPartsRoute().size()-1;p++) {
			for(int i=0;i<r.getPartsRoute().get(p).getListSubJobs().size();i++) {
				SubJobs nodeK=r.getPartsRoute().get(p).getListSubJobs().get(i);
				double startServiceTime=0;
				if(nodeK.isClient() ) {
					if(nodeK.getTotalPeople()<0 ) {
						startServiceTime=Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient());

						if(nodeK.getEndTime()>Math.max(nodeK.getStartTime(),nodeK.getArrivalTime())) {
							startServiceTime=Math.min(nodeK.getEndTime(),Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimeHomeCareStaff()));
						}}
					else {
						startServiceTime=Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient());

						if(nodeK.getEndTime()>Math.max(nodeK.getStartTime(),nodeK.getArrivalTime())) {
							startServiceTime=Math.min(nodeK.getEndTime(),Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()));		
						}
					}
				}
				if(nodeK.isMedicalCentre()) {

					if(nodeK.getTotalPeople()<0 ) {
						startServiceTime=Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient());

						if(nodeK.getEndTime()>Math.max(nodeK.getStartTime(),nodeK.getArrivalTime())) {
							startServiceTime=Math.min(nodeK.getEndTime(),Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient()+test.getRegistrationTime()));
						}}
					else {
						startServiceTime=Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient());

						if(nodeK.getEndTime()>Math.max(nodeK.getStartTime(),nodeK.getArrivalTime())) {
							startServiceTime=Math.min(nodeK.getEndTime(),Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()));		
						}}
				}
				if(nodeK.isPatient()) {
					if(nodeK.getTotalPeople()<0 ) {
						startServiceTime=Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient());

						if(nodeK.getEndTime()>Math.max(nodeK.getStartTime(),nodeK.getArrivalTime())) {
							startServiceTime=Math.min(nodeK.getEndTime(),Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient()));
						}}
					else {
						startServiceTime=Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()+test.getloadTimePatient());

						if(nodeK.getEndTime()>Math.max(nodeK.getStartTime(),nodeK.getArrivalTime())) {
							startServiceTime=Math.min(nodeK.getEndTime(),Math.max(nodeK.getStartTime(),nodeK.getArrivalTime()));		
						}}
				}
				r.getPartsRoute().get(p).getListSubJobs().get(i).setStartServiceTime(startServiceTime);
				r.getPartsRoute().get(p).getListSubJobs().get(i).setEndServiceTime(nodeK.getstartServiceTime()+r.getPartsRoute().get(p).getListSubJobs().get(i).getReqTime());

			}
		}
		//r.updateRouteFromParts(inp, test, jobsInWalkingRoute);

	}

	private void settingArrivalDepartureTime(Route r) {
		for(int p=1;p<r.getPartsRoute().size()-1;p++) {
			for(int i=1;i<r.getPartsRoute().get(p).getListSubJobs().size();i++) {
				SubJobs nodeK=r.getPartsRoute().get(p).getListSubJobs().get(i-1);
				SubJobs nodeL=r.getPartsRoute().get(p).getListSubJobs().get(i);
				double tv=inp.getCarCost().getCost(nodeK.getId()-1, nodeL.getId()-1);
				double arrivalTimeK=nodeL.getArrivalTime()-tv;
				nodeK.setarrivalTime(arrivalTimeK);
				if(nodeK.isClient()) {
					nodeK.setdepartureTime(nodeK.getArrivalTime()+test.getloadTimeHomeCareStaff());}
				else {
					nodeK.setdepartureTime(nodeK.getArrivalTime()+test.getloadTimePatient());
				}
			}}
		//r.updateRouteFromParts(inp, test, jobsInWalkingRoute);

	}

	private void mergingRoutes(ArrayList<Route> poolRoutes) {
		poolRoutes.sort(Route.SORT_BY_EarlyJob);
		ArrayList<Route> newRoutes= new ArrayList<Route>();


		for(int indexA=0;indexA<poolRoutes.size();indexA++) {
			for(int indexB=0;indexB<poolRoutes.size();indexB++) {

				Route route1= poolRoutes.get(indexA);
				Route route2= poolRoutes.get(indexB);
				if(route1!=route2 && !route1.getSubJobsList().isEmpty() && !route2.getSubJobsList().isEmpty()) {

					Route newRoute= merging(route1,route2);
					if(newRoute!=null) {
						route2.getSubJobsList().clear();
						route2.getPartsRoute().clear();
						route1.getSubJobsList().clear();
						route1.getPartsRoute().clear();
						for(SubJobs j:newRoute.getSubJobsList()) {
							route1.getSubJobsList().add(j);
						}
						for(Parts p:newRoute.getPartsRoute()) {
							route1.getPartsRoute().add(p);
						}

						route1.updateRouteFromParts(inp, test, jobsInWalkingRoute);
						route1.countingMedicalStaff();
						route1.setAmountParamedic(newRoute.getAmountParamedic());
						route1.setHomeCareStaff(newRoute.getHomeCareStaff());
					}
				}
			}
		}

		int stop=poolRoutes.size();

		for(int i=0;i<stop;i++) {
			for(Route newRoute:poolRoutes) {
				if(newRoute.getSubJobsList().isEmpty()) {
					poolRoutes.remove(newRoute);
					break;
				}
			}	
		}

		int stop2=poolRoutes.size();



	}

	private Route merging(Route route1, Route route2) {
		Route newRoute=null;
		ArrayList<SubJobs> jobsList= new ArrayList<SubJobs>(); 
		for(SubJobs j:  route1.getSubJobsList()) {
			jobsList.add(j);
		}
		for(SubJobs j:  route2.getSubJobsList()) {
			jobsList.add(j);	
		}
		jobsList.sort(SubJobs.SORT_BY_STARTW);
		ArrayList<SubJobs> sequenceJobs= new ArrayList<SubJobs>(); 
		double paramedics=route1.getAmountParamedic()+route2.getAmountParamedic();
		double homeCareStaff=route1.getHomeCareStaff()+route2.getHomeCareStaff();

		double CapVehicle=homeCareStaff+paramedics;

		boolean inserted=testing(jobsList);
		CapVehicle+=jobsList.get(0).getTotalPeople();
		sequenceJobs.add(jobsList.get(0));
		for(int i=1;i<jobsList.size();i++) {
			SubJobs Inode=jobsList.get(i-1);
			SubJobs Jnode=jobsList.get(i);
			double tv=inp.getCarCost().getCost(Inode.getId()-1, Jnode.getId()-1);
			double xxx=Inode.getDepartureTime()+tv;

			//if(Inode.getDepartureTime()+tv<=Jnode.getEndTime()) {
			sequenceJobs.add(Jnode);
			if(!testing(jobsList)) { // time window
				sequenceJobs.clear();
				break;
			}
		}

		if(!sequenceJobs.isEmpty()) {
			//newRoute=new Route();
			SettingTimesSequence(sequenceJobs);
			Parts p = new Parts();

			p.setListSubJobs(sequenceJobs, inp, test);

			ArrayList<Parts> pAux= new ArrayList<Parts>();
			pAux.add(p);
			ArrayList<Route> poolRoutes=insertingDepotConnections(pAux);
			newRoute=new Route(poolRoutes.get(0));
			//newRoute.getPartsRoute().add(route1.getPartsRoute().get(0));
			//			newRoute.getPartsRoute().add(p);
			//			newRoute.getPartsRoute().add(route1.getPartsRoute().get(route1.getPartsRoute().size()-1));
			newRoute.updateRouteFromParts(inp, test, jobsInWalkingRoute);
			newRoute.setAmountParamedic(paramedics);
			newRoute.setHomeCareStaff(homeCareStaff);
		}
		return newRoute;
	}


	private Solution createInitialSolution(int iter) {
		ArrayList<ArrayList<Couple>> clasification= clasificationjob(); // classification according to the job qualification
		assigmentJobsToQualifications(clasification);
		Solution solsorting = sortInsertionProcedure(iter,false);
		return new Solution(solsorting);
	}



	private Solution assigmentVehicle(Solution assigmentPersonnalJob) {
		// 1. cambio de tiempos
		Solution sol= changingTimesVehicleRoute(assigmentPersonnalJob);
		return sol;
	}




	private Solution changingTimesVehicleRoute(Solution assigmentPersonnalJob) {
		// 1. copia de la solución actual
		Solution currentSolution = new Solution (assigmentPersonnalJob);
		Solution newSolution = new Solution ();
		// 2. tratamiento de trabajos en cada ruta: definición de las ventanas de tiempo para cada trabajo hard

		for(Route r:currentSolution.getRoutes()) {
			treatmentRoute(r,newSolution);//changing TW
		}
		int i=-1;
		for(Route r:newSolution.getRoutes()) {
			i++;
			r.setIdRoute(i);
		}
		return newSolution;
	}

	private void treatmentRoute(Route r, Solution newSolution) {
		// 1. iterar sobre los trabajos con las duras ventanas de tiempo
		Route newRoute= new Route();
		newSolution.getRoutes().add(newRoute);
		// setting information personnal staff
		newRoute.setAmountParamedic(r.getAmountParamedic());
		newRoute.setHomeCareStaff(r.getHomeCareStaff());
		///
		double allowedMaxDeviation= Double.MAX_VALUE;
		newRoute.getPartsRoute().add(new Parts(r.getPartsRoute().get(0)));
		for(int p=1;p<r.getPartsRoute().size()-1;p++) {
			Parts pCopy= new Parts();
			for(SubJobs j:r.getPartsRoute().get(p).getListSubJobs()) {
				SubJobs copy= new SubJobs(j);
				if(copy.isClient() && copy.getTotalPeople()<0) { // 2. calcular la max time deviation
					double deviation=copy.getstartServiceTime()-copy.getStartTime();
					if(deviation>0 && deviation<allowedMaxDeviation) {
						allowedMaxDeviation=deviation;
					}
				}
				pCopy.getListSubJobs().add(copy);
			}
			for(Edge e:r.getPartsRoute().get(p).getDirectoryConnections().values()) {
				pCopy.getDirectoryConnections().put(e.getEdgeKey(), e);
			}
			newRoute.getPartsRoute().add(pCopy);
		}
		for(Edge e:r.getEdges().values()) {
			newRoute.getEdges().put(e.getEdgeKey(), e);
		}
		newRoute.getPartsRoute().add(new Parts(r.getPartsRoute().get(r.getPartsRoute().size()-1)));
		if(allowedMaxDeviation>test.getCumulativeWaitingTime()) {
			allowedMaxDeviation=test.getCumulativeWaitingTime();
		}
		for(Parts p:newRoute.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {
				j.setStartTime(Math.max(0, j.getstartServiceTime()-allowedMaxDeviation));
				j.setEndTime(j.getstartServiceTime());
			}
		}

		for(Parts p:newRoute.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {

				if(j.isClient() || j.isMedicalCentre()) {
					if(j.getTotalPeople()<0) {
						j.setdepartureTime(j.getArrivalTime()+j.getloadUnloadTime());		
					}	
				}
			}
		}

		// actualizar information parts
		// actualizar la hora de los drop-off


		newRoute.updateRouteFromParts(inp, test, jobsInWalkingRoute);

	}

	private Solution creatingPoolRoute(Solution sol1) {
		Solution sol= new Solution();

		// PACIETES drop off
		// 1. Creación de tantas rutas como trabajos se tienen 2. Cada trabajo se ubica en la mejor posición
		ArrayList<Parts> routesPool= generatingPoolRoutes();
		sol = selectingBestCombinationRoutes(routesPool,sol1);
		return sol;
	}

	private Solution selectingBestCombinationRoutes(ArrayList<Parts> routesPool, Solution sol1) {
		ArrayList<Route> route=insertingDepotConnections(routesPool);
		// creación de partes
		Solution newSol= solutionInformation(route); 

		newSol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
		for(Route r:sol1.getRoutes()) {
			newSol.getRoutes().add(r);
		}

		HashMap<String, SubJobs> missingJobs= new HashMap<String, SubJobs>();
		HashMap<String, SubJobs> sameJobs= new HashMap<String, SubJobs>();
		for(Route r:sol1.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				sameJobs.put(j.getSubJobKey(), j);
			}	
		}
		for(Route r:newSol.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				if(!sameJobs.containsKey(j.getSubJobKey())) {
					missingJobs.put(j.getSubJobKey(), j);}
			}	
		}


		//ExactDrivingRoutes xpressPoolRoutes= new ExactDrivingRoutes(sol1);
		ExactDrivingRoutes xpressPoolRoutes= new ExactDrivingRoutes(newSol);


		for(Route r: xpressPoolRoutes.getDrivingRoutes()) {

			r.totalMedicalStaff();
		}

		Solution sol= solutionInformation(xpressPoolRoutes.getDrivingRoutes()); 
		sol.checkingSolution(inp,test,jobsInWalkingRoute, initialSol);
		return sol;
	}

	private ArrayList<Parts> generatingPoolRoutes() {
		ArrayList<Parts> routesPool = new ArrayList<>();

		// 2. Iterativamente se intenta insertar

		ArrayList<SubJobs> listSubJobsDropOff= new ArrayList<SubJobs>();	
		for(Couple dropOff:dropoffpatientMedicalCentre.values()) {
			SubJobs pickUpPatient=(SubJobs)dropOff.getStartEndNodes().get(1);
			SubJobs dropOffPatient=(SubJobs)dropOff.getStartEndNodes().get(0);
			listSubJobsDropOff.add(dropOffPatient);
			Parts newRoute= new Parts();
			newRoute.getListSubJobs().add(pickUpPatient);
			newRoute.getListSubJobs().add(dropOffPatient);
			newRoute.getDirectorySubjobs().put(pickUpPatient.getSubJobKey(), pickUpPatient);
			newRoute.getDirectorySubjobs().put(dropOffPatient.getSubJobKey(), dropOffPatient);
			routesPool.add(newRoute);
		}
		listSubJobsDropOff.sort(Jobs.SORT_BY_STARTW);

		for(SubJobs j:listSubJobsDropOff) {
			Couple c= new Couple(dropoffpatientMedicalCentre.get(j.getSubJobKey()),inp,test);
			SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
			SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
			for(Parts paramedic:routesPool) {
				if(!paramedic.getDirectorySubjobs().containsKey(j.getSubJobKey())) {
					boolean insertesed=false;
					if(paramedic.getListSubJobs().isEmpty()) {
						insertesed=true;

						paramedic.getListSubJobs().add(present);
						paramedic.getListSubJobs().add(future);
						paramedic.getDirectorySubjobs().put(present.getSubJobKey(),present);
						paramedic.getDirectorySubjobs().put(future.getSubJobKey(),future);

						//break;
					}
					else { // iterating over the route
						insertesed=insertingPairSubJobsDropOffPickUpPatient(present,future,paramedic);
						//					if(insertesed) {
						//						break;
						//					}
					}
				}
			}
		}



		// PACIETES pick up
		ArrayList<SubJobs> listSubJobsPickUp= new ArrayList<SubJobs>();


		for(Couple dropOff:pickpatientMedicalCentre.values()) {
			SubJobs pickUpPatient=(SubJobs)dropOff.getStartEndNodes().get(1);
			listSubJobsPickUp.add(pickUpPatient);
		}
		listSubJobsPickUp.sort(Jobs.SORT_BY_STARTW);

		//	newRoutes
		for(SubJobs pickUp:listSubJobsPickUp) {
			boolean insertesed=false;

			for(Parts paramedic:routesPool) {
				if(paramedic.getDirectorySubjobs().containsKey(pickUp.getSubJobKey())) {
					if(paramedic.getListSubJobs().isEmpty()) {
						insertesed=true;
						Couple c= new Couple(pickpatientMedicalCentre.get(pickUp.getSubJobKey()),inp,test);
						SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
						SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
						paramedic.getListSubJobs().add(present);
						paramedic.getListSubJobs().add(future);
						paramedic.getDirectorySubjobs().put(present.getSubJobKey(),present);
						paramedic.getDirectorySubjobs().put(future.getSubJobKey(),future);
						//break;
					}
					else { // iterating over the route
						insertesed=insertingPairSubJobsPickUpDropOffPatient(pickUp,paramedic);
						//					if(insertesed) {
						//						break;
						//					}
					}
				}
			}
		}




		// CLIENTES drop off

		listSubJobsDropOff.clear();
		listSubJobsPickUp.clear();
		HashMap<String,SubJobs> pickUpDirectory= new HashMap<>();

		for(Couple dropOff:dropoffHomeCareStaff.values()) {
			SubJobs dropOffHomeHealthCare=(SubJobs)dropOff.getStartEndNodes().get(1);
			listSubJobsDropOff.add(dropOffHomeHealthCare);
		}
		for(Couple dropOff:pickUpHomeCareStaff.values()) {
			SubJobs pickUpHomeHealthCare=(SubJobs)dropOff.getStartEndNodes().get(0);
			listSubJobsPickUp.add(pickUpHomeHealthCare);
			pickUpDirectory.put(pickUpHomeHealthCare.getSubJobKey(), pickUpHomeHealthCare);
		}


		listSubJobsDropOff.sort(Jobs.SORT_BY_STARTW);
		listSubJobsPickUp.sort(Jobs.SORT_BY_STARTW);

		// clients

		for(SubJobs j:listSubJobsDropOff) {

			boolean insertesed=false;
			boolean secondPart=false;
			Couple c= new Couple(dropoffHomeCareStaff.get(j.getSubJobKey()), inp,test);
			SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
			SubJobs pickUp=(SubJobs)c.getStartEndNodes().get(0);
			for(Parts paramedic:routesPool) {
				if(paramedic.getListSubJobs().isEmpty()) {

					insertesed=true;
					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(pickUp);
					paramedic.getDirectorySubjobs().put(present.getSubJobKey(), present);
					paramedic.getDirectorySubjobs().put(pickUp.getSubJobKey(), pickUp);
					pickUpDirectory.remove(pickUp.getSubJobKey());

					//break;
				}
				else { // iterating over the route
					insertesed=insertingPairSubJobsDropOffClient(j,paramedic);

					if(insertesed) {
						secondPart=insertingPairSubJobsPickUpClient(pickUp,paramedic);
						if(secondPart) {
							pickUpDirectory.remove(pickUp.getSubJobKey());
						}
					}
				}
			}

			if(!insertesed) {

				Parts newPart=new Parts();
				newPart.getListSubJobs().add(present);
				newPart.getListSubJobs().add(pickUp);
				newPart.getDirectorySubjobs().put(present.getSubJobKey(),present);
				newPart.getDirectorySubjobs().put(pickUp.getSubJobKey(),pickUp);
				pickUpDirectory.remove(pickUp.getSubJobKey());

				routesPool.add(newPart);
				//break;
			}
		}
		//newRoutes
		//	routesPool.add(newPart);


		// CLIENTES pick up

		for(SubJobs pickUp:listSubJobsPickUp) {

			if(!pickUpDirectory.containsKey(pickUp.getSubJobKey())) {
				boolean insertesed=false;
				for(Parts paramedic:routesPool) {
					Couple c= new Couple(pickUpHomeCareStaff.get(pickUp.getSubJobKey()),inp,test);
					SubJobs present=(SubJobs)c.getStartEndNodes().get(0);
					//SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
					if(paramedic.getListSubJobs().isEmpty()) {
						insertesed=true;
						paramedic.getListSubJobs().add(present);
						paramedic.getDirectorySubjobs().put(present.getSubJobKey(),present);

						//break;
					}
					else { // iterating over the route
						insertesed=insertingPairSubJobsPickUpClient(pickUp,paramedic);
						if(insertesed) {
							//break;
						}
					}
				}
				if(!insertesed) {

					Parts newPart=new Parts();
					newPart.getListSubJobs().add(pickUp);
					newPart.getDirectorySubjobs().put(pickUp.getSubJobKey(),pickUp);
					//System.out.println("Stop");
					routesPool.add(newPart);
					//	break;

				}
			}
		}


		integratingPools(routesPool);
		return routesPool;
	}

	private void integratingPools(ArrayList<Parts> routesPool) {
		ArrayList<Parts> newRoutes = new ArrayList<>();
		for(Parts p: routesPool) {
			newRoutes.add(p);
		}
		HashMap<String,SubJobs> totalJobs= new HashMap<>();
		for(Parts p1: newRoutes) {// merging route
			for(Parts p2: newRoutes) {
				if(p1!=p2) {
					totalJobs= selectingListSubJobs(p1,p2);
					ArrayList<SubJobs> list=gettingSubJobsList(p1,p2);
					ArrayList<SubJobs> route=new ArrayList<SubJobs>();
					HashMap<String,SubJobs> jobDirectory= new HashMap<>();
					route.add(list.get(0));
					for(int i=1;i<list.size();i++) {
						SubJobs jobI= list.get(i-1);
						SubJobs jobJ=list.get(i);
						if(!jobDirectory.containsKey(jobJ.getSubJobKey())) {
							double tv=inp.getCarCost().getCost(jobI.getId()-1, jobJ.getId()-1);
							if(jobI.getDepartureTime()+tv<=jobJ.getArrivalTime() ) {
								route.add(jobJ);
								jobDirectory.put(jobJ.getSubJobKey(), jobJ);
								if(!vehicleCapacityPart(route)) {
									route.remove(jobJ);
									jobDirectory.remove(jobJ.getSubJobKey());
									break;
								}
							}
							else {
								break;
							}
						}
						if(route.size()==totalJobs.size()) {
							Parts p= new Parts();
							p.setListSubJobs(route, inp, test);
							routesPool.add(p);
						}

					}
				}
			}	
		}
	}

	private HashMap<String, SubJobs> selectingListSubJobs(Parts p1, Parts p2) {
		HashMap<String, SubJobs> list= new HashMap<String, SubJobs>();
		for(SubJobs j:p1.getListSubJobs()) {
			if(!list.containsKey(j.getSubJobKey())) {
				list.put(j.getSubJobKey(), j);}
		}
		for(SubJobs j:p2.getListSubJobs()) {
			if(!list.containsKey(j.getSubJobKey())) {
				list.put(j.getSubJobKey(), j);}
		}
		return list;
	}

	private ArrayList<SubJobs> gettingSubJobsList(Parts p1, Parts p2) {
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs> ();
		for(SubJobs j:p1.getListSubJobs()) {
			subJobsList.add(j);
		}
		for(SubJobs j:p2.getListSubJobs()) {
			subJobsList.add(j);
		}
		subJobsList.sort(Jobs.SORT_BY_STARTSERVICETIME);
		return subJobsList;
	}


	Solution assigningRoutesToDrivers(int iteration, Solution initialSol) {
		Solution newSol=null;
		//Solution newSol=new Solution(initialSol);
		Solution startingSol=new Solution(initialSol);

		ArrayList<Jobs> missingJobs= checkingMissingJobs(startingSol);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}
		Solution neighborhood = neighborhoods(iteration,startingSol);


		ArrayList<Route> routeList= new ArrayList<Route>();


		if(neighborhood.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
			newSol=new Solution (neighborhood);
		}
		else {
			newSol=new Solution (startingSol);
		}
		//
		//		boolean goodSolution=solutionValitadion(newSol);
		//
		//
		//		if(!goodSolution) {
		//			//System.out.println("Stop");
		//		}

		return newSol;


	}


	private void updatingShifts(Solution newSol) {
		for(Route r:newSol.getShift().getRoutes()) {
			r.updateRouteFromParts(inp, test, jobsInWalkingRoute);
		}
	}

	private Solution shiftDefinition(int iteration, Solution diversifiedSolneighborhood) {
System.out.println(diversifiedSolneighborhood.toString());
		
		// to remove
		HashMap<String, Couple> dropoffHomeCareStaff= selectingHomeCareStaffCouple(diversifiedSolneighborhood);// hard time windows list of home care staff 
		HashMap<String, Couple> pickupHomeCareStaff= selectingHomeCareStaffPickUpCouple(dropoffHomeCareStaff,diversifiedSolneighborhood);
		HashMap<String, Couple> dropoffpatientMedicalCentre= selectingCoupleDropOffMedical(diversifiedSolneighborhood);// hard time windows list of patient
		HashMap<String, Couple> pickpatientMedicalCentre= selectingCouplepickpatientMedicalCentre(dropoffpatientMedicalCentre,diversifiedSolneighborhood);// soft time windows list of patient
		if(dropoffpatientMedicalCentre.isEmpty()) {
			dropoffpatientMedicalCentre= selectingCoupleDropOffMedical(diversifiedSolneighborhood);// hard time windows list of patient
		}
		ArrayList<Route> poolRoutes = new ArrayList<Route>();
		for(Route r:diversifiedSolneighborhood.getRoutes()) {
			poolRoutes.add(r);
		}
		//System.out.println("Iteration # " +iteration);
		ArrayList<Parts> shifts= assigmentShifts(poolRoutes,dropoffHomeCareStaff,pickupHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre);
		ArrayList<Route> poolshifts=insertingDepotConnections(shifts);

		Solution shift= new Solution ();
		for(Route route:poolshifts) {
			if(!route.getPartsRoute().isEmpty()) {
				route.updateRouteFromParts(inp, test, jobsInWalkingRoute);
				shift.getRoutes().add(route);}
		}
		//System.out.println("Shift information");
		//System.out.println(shift.toString());
		ArrayList<Jobs> missingJobs= checkingMissingJobs(shift);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}
		for(Route r: shift.getRoutes()) {
			r.settingConnections(diversifiedSolneighborhood,test,inp);
		}

		checkingIntermediateStopsDepot(shift,diversifiedSolneighborhood);

		for(Route r: shift.getRoutes()) {
			r.settingEdgesTimesRoute(diversifiedSolneighborhood,test,inp);
			r.checkingWaitingTimes(test,inp);
			//	r.checkingDetour();
			r.checkingDetour(test,inp,diversifiedSolneighborhood);	
			r.computeHomCareStaffCost(test,inp);
		}

		double travelTime=0;
		double waitingTime=0;
		double detour=0;
		double detourViolation=0;
		double detourHHC=0;
		double detourParamedic=0;
		double medicalStaffcost=0;
		double paramedics=0;
		double homeCareStaff=0;
		for(Route r:shift.getRoutes() ) {
			paramedics+=r.getAmountParamedic();
			homeCareStaff+=r.getHomeCareStaff();
			travelTime+=r.getTravelTime();
			waitingTime+=r.getWaitingTime();
			detour+=r.getDetour();
			detourViolation+=r.getdetourViolation();
			detourHHC+=r.getdetourPromHomeCareStaff();
			detourParamedic+=r.getdetourPromParamedic();
			medicalStaffcost+=r.gethomeCareStaffCost();
		}
		shift.sethomeCareStaffTravelTime(travelTime);
		shift.setdetourDuration(detour);
		shift.setWaitingTime(waitingTime);
		shift.setdetourPromHomeCareStaff(detourHHC/homeCareStaff);
		shift.setdetourPromParamedics(detourParamedic/paramedics);
		shift.setdetourViolation(detourViolation);
		shift.setdetourDuration(medicalStaffcost);
		return shift;
	}

	private void checkingIntermediateStopsDepot(Solution shift, Solution diversifiedSolneighborhood) {
		ArrayList<Parts> parts= new ArrayList<Parts>();
		for(Route r:diversifiedSolneighborhood.getRoutes()) {
			for(Parts p: r.getPartsRoute()) {
				if(p.getListSubJobs().get(0).getId()!=1) {
					parts.add(p);
				}			
			}
		}
		ArrayList<Parts> newShift= new ArrayList<Parts>();
		SubJobs depotDropOff= new SubJobs(diversifiedSolneighborhood.getRoutes().get(0).getPartsRoute().get(diversifiedSolneighborhood.getRoutes().get(0).getPartsRoute().size()-1).getListSubJobs().get(0));
		depotDropOff.setarrivalTime(0);
		depotDropOff.setStartServiceTime(0);
		depotDropOff.setEndServiceTime(0);
		depotDropOff.setdepartureTime(0);
		SubJobs depotPickUp= new SubJobs(diversifiedSolneighborhood.getRoutes().get(0).getPartsRoute().get(0).getListSubJobs().get(0));
		depotPickUp.setarrivalTime(0);
		depotPickUp.setStartServiceTime(0);
		depotPickUp.setEndServiceTime(0);
		depotPickUp.setdepartureTime(0);
		for(Route r:shift.getRoutes()) {
			ArrayList<Edge> edgesList= new ArrayList<Edge>(); 
			ArrayList<SubJobs> list= new ArrayList<SubJobs>(); 
			for(Edge e:r.getEdges().values()) {
				SubJobs origin=e.getOrigin();
				SubJobs end=e.getEnd();
				if(origin.getId()!=1 && end.getId()!=1) {			
					Parts p1=selectionParts(origin,parts);
					Parts p2=selectionParts(end,parts);
					if(p1!=p2) { // hay una parata en el depot	
						Edge e1= new Edge(origin,depotDropOff,inp,test);
						Edge e2= new Edge(depotPickUp,end,inp,test);
						edgesList.add(e1);
						edgesList.add(e2);
					}
					else {
						edgesList.add(e);
					}}
				else {
					edgesList.add(e);
				}
			}
			r.getEdges().clear();
			for(Edge e:edgesList) {
				r.getEdges().put(e.getEdgeKey(), e);
			}
			//System.out.println(newShift.toString());
		}
	}

	private Parts selectionParts(SubJobs end, ArrayList<Parts> parts) {
		Parts chain=null;	
		for(Parts p: parts) {
			if(p.getDirectorySubjobs().containsKey(end.getSubJobKey())) {
				chain=p;	
			}
		}
		return chain;
	}

	private Solution checkSolution(HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {
		Solution sol= new Solution();
		SubJobs depot =new SubJobs(inp.getNodes().get(0));

		for(Couple c: dropoffHomeCareStaff2.values()) {	 // home care staff
			LinkedList<SubJobs> list= new LinkedList<>();
			HashMap<String, Edge> edgesList= new HashMap<String, Edge>();

			list.add(depot);
			SubJobs i = new SubJobs(c.getPresent());
			Edge e= new Edge(depot,i,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			SubJobs j = new SubJobs(c.getFuture());
			e= new Edge(i,j,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			e= new Edge(j,depot,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			list.add(i);
			list.add(j);
			list.add(depot);
			Route r = new Route();
			r.setSubJobsList(list);
			Parts p= new Parts();
			for(SubJobs sub: list) {
				p.getListSubJobs().add(sub);
			}
			r.getPartsRoute().add(p);
			r.setEdges(edgesList);
			sol.getRoutes().add(r);
		}

		for(Couple c: dropoffpatientMedicalCentre2.values()) {	 // home care staff
			LinkedList<SubJobs> list= new LinkedList<>();
			HashMap<String, Edge> edgesList= new HashMap<String, Edge>();

			list.add(depot);
			SubJobs i = new SubJobs(c.getPresent());
			Edge e= new Edge(depot,i,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			SubJobs j = new SubJobs(c.getFuture());
			e= new Edge(i,j,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			e= new Edge(j,depot,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			list.add(i);
			list.add(j);
			String key="D"+i.getId();
			Couple c1=pickpatientMedicalCentre2.get(key);
			SubJobs k = new SubJobs(c1.getPresent());
			e= new Edge(j,i,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			SubJobs l = new SubJobs(c1.getFuture());
			e= new Edge(k,l,inp,test);
			edgesList.put(e.getEdgeKey(), e);
			e= new Edge(l,depot,inp,test);
			edgesList.put(e.getEdgeKey(), e);


			list.add(k);
			list.add(l);


			list.add(depot);
			Route r = new Route();
			r.setSubJobsList(list);
			Parts p= new Parts();
			for(SubJobs sub: list) {
				p.getListSubJobs().add(sub);
			}
			r.getPartsRoute().add(p);
			r.setEdges(edgesList);
			sol.getRoutes().add(r);
		}

		sol.checkingSolution(inp, test, jobsInWalkingRoute, sol);
		// computing detour
		double detour=0;
		for(Route r:sol.getRoutes()) {
			for(Edge e:r.getEdges().values()) {
				detour +=e.getDetour()-e.getTime();
			}	
		}
		sol.setdetourDuration(detour);
		return sol;
	}

	private HashMap<String, Couple> selectingCouplepickpatientMedicalCentre(
			HashMap<String, Couple> dropoffpatientMedicalCentre2, Solution diversifiedSolneighborhood) {
		HashMap<String, Couple> pickpatientMedicalCentre= new HashMap<>();// soft time windows list of patient

		for(Couple c:this.pickpatientMedicalCentre.values()) {
			String key="D"+c.getPresent().getId()+c.getPresent().getIdUser();

			//
			SubJobs j=new SubJobs(c.getPresent());
			SubJobs jx=new SubJobs(c.getFuture());
			SubJobs startNode=selectingNode(j,diversifiedSolneighborhood);
			SubJobs endNode=selectingNode(jx,diversifiedSolneighborhood);
			Couple paar=new Couple(startNode,endNode);
			if(endNode.getIdUser()==0) {
				//System.out.println("Stop");
			}
			if(startNode.getIdUser()==0) {
				//System.out.println("Stop");
			}
			pickpatientMedicalCentre.put(endNode.getSubJobKey(), paar);
		}
		return pickpatientMedicalCentre;
	}

	private HashMap<String, Couple> selectingHomeCareStaffPickUpCouple(HashMap<String, Couple> dropoffHomeCareStaff2, Solution sol) {
		HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff 
		for(Couple c:dropoffHomeCareStaff2.values()) {
			SubJobs j=new SubJobs(c.getPresent());
			SubJobs jx=new SubJobs(c.getFuture());
			SubJobs present=selectingNode(j,sol);
			SubJobs endNode=selectingNode(jx,sol);
			Couple paar=new Couple(present,endNode);
			dropoffHomeCareStaff.put(endNode.getSubJobKey(), paar);
		}
		return dropoffHomeCareStaff;
	}


	private Solution neighborhoods(int iteration, Solution solsorting) {


		Solution newSol= reducingVehicles(iteration,solsorting); // todo para solucionar los detours


		ArrayList<Jobs> missingJobs= checkingMissingJobs(newSol);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}

		//System.out.println("Solution iteration" + solsorting.toString());

		//System.out.println("Solution before inter changes" + newSol);
		//System.out.println("Solution iteration" + iteration);
		iteration=2;
		Solution vecindario2= movement(iteration,newSol);

		if(vecindario2.getobjectiveFunction()<=solsorting.getobjectiveFunction()) {
			newSol=new Solution(vecindario2);
		}
		else {
			newSol=new Solution(solsorting);
		}
		missingJobs= checkingMissingJobs(vecindario2);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}
		Solution vecindario3= detourImprovement(iteration,newSol);
System.out.println(vecindario3.toString());

if(vecindario3.getobjectiveFunction()<=newSol.getobjectiveFunction()) {
	newSol=new Solution(vecindario3);
}
		Solution vecindario4= detourFromDepotImprovement(iteration,newSol);

		//Solution vecindario3= chain(iteration,newSol);

		if(vecindario4.getobjectiveFunction()<=newSol.getobjectiveFunction()) {
			newSol=new Solution(vecindario4);
		}

		missingJobs= checkingMissingJobs(vecindario3);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}

		//				if(vecindario3.getobjectiveFunction()<=vecindario2.getobjectiveFunction()) {
		//					newSol=new Solution(vecindario3);
		//				}
		//				else {
		//					newSol=new Solution(vecindario2);
		//				}

		missingJobs= checkingMissingJobs(newSol);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}
		return newSol;
	}

	private Solution detourFromDepotImprovement(int iteration, Solution currentSol) {

		Solution newSol= null;

		// seleccionar la lista de couples
		HashMap<String, Couple> dropoffHomeCareStaff= selectingHomeCareStaffCouple(currentSol);// hard time windows list of home care staff 
		HashMap<String, Couple> pickupHomeCareStaff= selectingHomeCareStaffPickUpCouple(dropoffHomeCareStaff,currentSol);
		HashMap<String, Couple> dropoffpatientMedicalCentre= selectingCoupleDropOffMedical(currentSol);// hard time windows list of patient
		HashMap<String, Couple> pickpatientMedicalCentre= selectingCouplepickpatientMedicalCentre(dropoffpatientMedicalCentre,currentSol);// soft time windows list of patient

		//dropoffHomeCareStaff,pickupHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre
		// seleccionar detours

		ArrayList<Edge> edgesList= selectionEdgesDetourFromDepot(currentSol); // seleccionar los ejes con detours<- determinan los transport request to remove
		HashMap<String, SubJobs> listFromDepot= new HashMap<>();
		HashMap<String, SubJobs> listToDepot= new HashMap<>();

		for(Edge e: edgesList) { // son ejes que conectan al depot
			if(e.getOrigin().getId()==1) {
				listFromDepot.put(e.getEnd().getSubJobKey(), e.getEnd());
			}
		}

		for(Edge e: edgesList) { // son ejes que conectan al depot
			if(e.getEnd().getId()==1) {
				listToDepot.put(e.getOrigin().getSubJobKey(), e.getOrigin());
			}
		}


		HashMap<String,SubJobs> list= new HashMap<String,SubJobs> ();

		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs> (); // 1) se tratan primero los detours desde el depot

		for(SubJobs j: listFromDepot.values()) {
			if(j.getSubJobKey().equals("D4871")) {
				System.out.println(j.toString());
			}
			if(j.isPatient() && j.getTotalPeople()<0) {
				SubJobs jPresent = new SubJobs(pickpatientMedicalCentre.get(j.getSubJobKey()).getPresent());
				System.out.println(jPresent.toString());
				list.put(jPresent.getSubJobKey(), jPresent);
				list.put(j.getSubJobKey(), j);
				//	subJobsList.add(jPresent);
			}
			else {
				if(j.isPatient() && j.getTotalPeople()>0) {
					String key= "D"+j.getsubJobPair().getId()+j.getId();
					SubJobs jPresent = new SubJobs(dropoffpatientMedicalCentre.get(key).getFuture());
					System.out.println(jPresent.toString());
					///subJobsList.add(jPresent);
					list.put(jPresent.getSubJobKey(), jPresent);
					list.put(j.getSubJobKey(), j);
				}
				else {
					if(j.isMedicalCentre() && j.getTotalPeople()<0) {
						SubJobs jPresent = new SubJobs(dropoffpatientMedicalCentre.get(j.getSubJobKey()).getPresent());
						list.put(jPresent.getSubJobKey(), jPresent);
						list.put(j.getSubJobKey(), j);
					}
					else {
						if(j.isMedicalCentre() && j.getTotalPeople()>0) {
							String key= "D"+j.getIdUser();
							SubJobs jPresent = new SubJobs(pickpatientMedicalCentre.get(key).getFuture());
							list.put(jPresent.getSubJobKey(), jPresent);
							list.put(j.getSubJobKey(), j);
						}
					else {
						list.put(j.getSubJobKey(), j);}
					}
				}
			}

		}


		for(SubJobs j: listToDepot.values()) {
			if(j.getSubJobKey().equals("D4871")) {
				System.out.println(j.toString());
			}
			if(j.isPatient() && j.getTotalPeople()<0) {
				SubJobs jPresent = new SubJobs(pickpatientMedicalCentre.get(j.getSubJobKey()).getPresent());
				System.out.println(jPresent.toString());
				list.put(jPresent.getSubJobKey(), jPresent);
				list.put(j.getSubJobKey(), j);
				//	subJobsList.add(jPresent);
			}
			else {
				if(j.isPatient() && j.getTotalPeople()>0) {
					String key= "D"+j.getsubJobPair().getId()+j.getId();
					SubJobs jPresent = new SubJobs(dropoffpatientMedicalCentre.get(key).getFuture());
					System.out.println(jPresent.toString());
					///subJobsList.add(jPresent);
					list.put(jPresent.getSubJobKey(), jPresent);
					list.put(j.getSubJobKey(), j);
				}
				else {
					if(j.isMedicalCentre() && j.getTotalPeople()<0) {
						SubJobs jPresent = new SubJobs(dropoffpatientMedicalCentre.get(j.getSubJobKey()).getPresent());
						list.put(jPresent.getSubJobKey(), jPresent);
						list.put(j.getSubJobKey(), j);
					}
					else {
						if(j.isMedicalCentre() && j.getTotalPeople()>0) {
							String key= "D"+j.getIdUser();
							SubJobs jPresent = new SubJobs(pickpatientMedicalCentre.get(key).getFuture());
							list.put(jPresent.getSubJobKey(), jPresent);
							list.put(j.getSubJobKey(), j);
						}
					else {
						list.put(j.getSubJobKey(), j);}
					}
				}
			}

		}


		subJobsList.clear();
		for(SubJobs j: list.values()) {
			subJobsList.add(j);			
		}

		newSol= dealingDetourFromDepot(subJobsList,currentSol,dropoffHomeCareStaff,pickupHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre);



		////////////////////////////////////////////////////////

		boolean goodSolution=solutionValitadion(newSol);
		return newSol;
	}


	private Solution dealingDetourFromDepot(ArrayList<SubJobs> subJobs, Solution currentSol, HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> pickupHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2) {
		Solution newSol= null;
		System.out.println("To print");
		System.out.println(currentSol.toString());

		ArrayList<Route> routeList= selectRouteCurrentSolution(currentSol,subJobs);
		ArrayList<Route> routeToChange=selectRouteRelatedTransportRequest(routeList); // seleccionar rutas de la solución actual
		ArrayList<Route> changedRoute=removingTransportRequestfromCurrentSolution(routeToChange,subJobs);// eliminar los transport requests
		ArrayList<Route> remainingRoute=selectingRemainingRoute(routeList,routeToChange);
		ArrayList<Parts> parts=selectingCurrentParts(remainingRoute,changedRoute);

		for(Parts p:parts ) {
		if(p.getDirectorySubjobs().containsKey("D5174")) {
			System.out.println(p.toString());
		}
		if(p.getDirectorySubjobs().containsKey("P74")) {
			System.out.println(p.toString());
		}}
		ArrayList<Route> poolRoutes=insertingDepotConnections(parts);
		boolean goodSolution=validationSequenceRoutes(poolRoutes);	 // aca da false porque se han extraido trabajos

		boolean feasibleStaff=false;
		do {
			int iter=-1;
			int amountVehicles=Integer.MAX_VALUE;
			while(inp.getVehicles().get(0).getQuantity()<amountVehicles) {

				if(iter<10) {

					newSol= new Solution();
					iter++;
					if(iter>100) {
						System.out.print(iter);
					}
					HashMap<String,SubJobs> toInsert= new HashMap<String,SubJobs>();
					ArrayList<SubJobs> insertionOrder=sortingJobsList(subJobs);

					ArrayList<Parts> poolParts= new ArrayList<Parts>();
					for(Parts p: parts) {
						poolParts.add(new Parts(p));
					}
					// pool of routes
					// ArrayList<Route> poolRoutes=insertingDepotConnections(parts);
					poolRoutes=insertingDepotConnections(parts);
					Solution ux=new Solution();
					for(Route r: poolRoutes) {
						ux.getRoutes().add(r);
					}
					ArrayList<Jobs> missingJobs= checkingMissingJobs(ux);
					if(missingJobs.isEmpty()) {
						System.out.println("Sol");
					}


					////////

					newSol= constructionSolFromDepot(insertionOrder,poolParts,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,pickupHomeCareStaff2,toInsert);

					//newSol= constructionSol(insertionOrder,poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,pickupHomeCareStaff,toInsert);
					amountVehicles=newSol.getRoutes().size();


				}
				else {break;}
			}
			ArrayList<Jobs> missingJobs= checkingMissingJobs(newSol);
			//System.out.println(newSol.toString());
			if(missingJobs.isEmpty()) {
				int iteration=2;
				Solution shift= shiftDefinition(iteration,newSol);
				shift.setShift(shift);
				newSol.setShift(shift);
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, shift);
				//System.out.println(newSol.toString());
				goodSolution=solutionValitadion(shift);
				goodSolution=solutionValitadion(newSol);
				feasibleStaff=checkingFeasibility(newSol);
				if(!feasibleStaff) {
					newSol= new Solution(currentSol);
					newSol.setShift(currentSol.getShift());
					newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
				}}
			else {
				//System.out.println(newSol.toString());
				newSol= new Solution(currentSol);
				newSol.setShift(currentSol.getShift());
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
			}
			feasibleStaff=checkingFeasibility(newSol);

		}while(!feasibleStaff);


		return newSol;
	}

	private Solution constructionSolFromDepot(ArrayList<SubJobs> insertionOrder, ArrayList<Parts> poolParts,
			HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2,
			HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff2,
			HashMap<String, SubJobs> toInsert) {
		HashMap<String, SubJobs> backUp = new HashMap<String, SubJobs>();
		boolean hard= true;
		boolean hardConstraints= false;
		HashMap<String, SubJobs> check = new HashMap<String, SubJobs>();
		for(SubJobs j: insertionOrder) {
			check.put(j.getSubJobKey(), j);
		}
		do {
			for(Jobs j: insertionOrder) {
				if(j.getSubJobKey().equals("D5174")) {
					System.out.print(j.toString());
				}
				if(j.getSubJobKey().equals("D42")) {
					System.out.print(j.toString());
				}
				boolean inserted= insertionJobSelectingRouteFromDepotDetour(poolParts,j,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,pickupHomeCareStaff2,toInsert,hardConstraints);
				//	boolean inserted2= insertionJobSelectingRoute(poolParts,j,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,pickupHomeCareStaff2,toInsert,hardConstraints);
				if(inserted) {
					insertionOrder.remove(j);
					toInsert.clear();
					//insertionOrder.sort(Jobs.TWSIZE_Early);
					int sortCriterion=rn.nextInt(2)+1;// 1 distance depot // 2 early TW // 3 size 
					switch(sortCriterion){
					case 1:  insertionOrder.sort(Jobs.SORT_BY_STARTW);
					break;
					case 2:  insertionOrder.sort(Jobs.TWSIZE_Early);
					break;
					}
					break;
				}
			}
			if(insertionOrder.isEmpty() && hard) {
				calculatingTW(poolParts,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,backUp);

				insertingSoftConstraints(backUp,poolParts,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2);
				for(SubJobs j:backUp.values()) {
					if(j.isClient()||j.isMedicalCentre()) {
						insertionOrder.add(j);}
				}
				hard=false;
				backUp.clear();


			}
			ArrayList<Jobs> missingJobs= checkingMissingJobs(poolParts);

		}while(!insertionOrder.isEmpty());
		//	slackMethod(poolParts);
		//mergingParts(poolParts);  // merging Parts
		ArrayList<Jobs> missingJobs= checkingMissingJobs(poolParts);
		ArrayList<Route> poolRoutes=insertingDepotConnections(poolParts);
		boolean goodSolution=validationSequenceRoutes(poolRoutes);	
		mergingRoutes(poolRoutes);

		goodSolution=validationSequenceRoutes(poolRoutes);		
		goodSolution=validationSequenceRoutes(poolRoutes);
		//slackTime(poolRoutes);
		stackingRoutes(poolRoutes);
		goodSolution=validationSequenceRoutes(poolRoutes);
		poolRoutes.sort(Route.SORT_BY_EarlyJob);

		//		boolean goodSolution=validationSequenceRoutes(poolRoutes);
		//		//System.out.println("Routes list ");
		//		for(Route route:poolRoutes) {
		//			//System.out.println("Route "+ route.toString());
		//		}

		Solution solution= new Solution ();
		for(Route route:poolRoutes) {
			if(!route.getPartsRoute().isEmpty()) {
				solution.getRoutes().add(route);}
		}
		solution.checkingSolution(inp, test, jobsInWalkingRoute, solution);
		goodSolution=validationSequenceRoutes(poolRoutes);
		//System.out.println(solution.toString());

		missingJobs= checkingMissingJobs(solution);
		//		Route r1=selectionRoute(null,solution);
		//		Route r2=selectionRoute1(null,solution);
		//solution.checkingSolution(inp, test, jobsInWalkingRoute, solution);
		return solution;
	}

	private boolean insertionJobSelectingRouteFromDepotDetour(ArrayList<Parts> poolParts, Jobs j,
			HashMap<String, Couple> dropoffHomeCareStaff2, HashMap<String, Couple> dropoffpatientMedicalCentre2,
			HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff2,
			HashMap<String, SubJobs> toInsert, boolean hardConstraints){
		boolean inserted= false;
		ArrayList<Parts> bestRoutes=selectionBestParst(j,poolParts);
		Parts changing=null;

		for(Parts p: bestRoutes) {
			if(p.getDirectorySubjobs().containsKey("D5174")) {
				System.out.println(p.toString());
			}
			if(p.getDirectorySubjobs().containsKey("P74")) {
				System.out.println(p.toString());
			}
			inserted=inseringRouteFromDepot(j,p,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,pickupHomeCareStaff2,hardConstraints);
			if(p.getDirectorySubjobs().containsKey("D5174")) {
				System.out.println(p.toString());
			}
			if(p.getDirectorySubjobs().containsKey("P74")) {
				System.out.println(p.toString());
			}
			if(inserted) {
				if(p.getDirectorySubjobs().containsKey("D5174")) {
					System.out.println(p.toString());
				}
				if(p.getDirectorySubjobs().containsKey("P74")) {
					System.out.println(p.toString());
				}
				changing=p;
				break;
			}
		}
		if(!inserted) {
			Parts p= new Parts();
			inserted=inseringRouteFromDepot(j,p,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,pickupHomeCareStaff2,hardConstraints);
			poolParts.add(p);
			changing=p;
		}

		updatingTimesCouples(poolParts,changing,dropoffHomeCareStaff2,dropoffpatientMedicalCentre2,pickpatientMedicalCentre2,toInsert);


		// update times for future
		return inserted;
	}
	private boolean inseringRouteFromDepot(Jobs j, Parts p, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> pickpatientMedicalCentre2,
			HashMap<String, Couple> pickupHomeCareStaff2, boolean hardConstraints) {
		boolean inserted= false;
		ArrayList<SubJobs> subJobsList= listJobAssociatedToJ(j,dropoffpatientMedicalCentre2,dropoffHomeCareStaff2,pickpatientMedicalCentre2,pickupHomeCareStaff2);
		if(p.getListSubJobs().isEmpty() ) {
			inserted= true;
			if(subJobsList.get(0).isPatient()) { // pick up 
				//if(subJobsList.get(0).getTotalPeople()>0) { // pick up 
				subJobsList.get(0).setStartServiceTime(subJobsList.get(0).getStartTime());
				subJobsList.get(0).setarrivalTime(subJobsList.get(0).getstartServiceTime());
				subJobsList.get(0).setdepartureTime(subJobsList.get(0).getArrivalTime()+test.getloadTimePatient());
				subJobsList.get(0).setEndServiceTime(subJobsList.get(0).getDepartureTime());

				//}
			}
			if(subJobsList.get(0).isMedicalCentre() || subJobsList.get(0).isClient() ) {
				if(subJobsList.get(0).getTotalPeople()<0) { // pick up 
					subJobsList.get(0).setStartServiceTime(subJobsList.get(0).getSoftStartTime());
					subJobsList.get(0).setarrivalTime(subJobsList.get(0).getstartServiceTime());
					subJobsList.get(0).setdepartureTime(subJobsList.get(0).getArrivalTime()+test.getloadTimePatient());

					subJobsList.get(0).setEndServiceTime(subJobsList.get(0).getstartServiceTime()+subJobsList.get(0).getReqTime());
				}
				else {
					{ // pick up 
						subJobsList.get(0).setStartServiceTime(subJobsList.get(0).getStartTime());
						subJobsList.get(0).setarrivalTime(subJobsList.get(0).getstartServiceTime());
						subJobsList.get(0).setdepartureTime(subJobsList.get(0).getArrivalTime()+test.getloadTimePatient());
						subJobsList.get(0).setEndServiceTime(subJobsList.get(0).getDepartureTime());

					}
				}
			}
			p.getListSubJobs().add(subJobsList.get(0));
			p.getDirectorySubjobs().put(subJobsList.get(0).getSubJobKey(), subJobsList.get(0));
			for(int index=1;index<subJobsList.size();index++) {
				SubJobs a=subJobsList.get(index-1);
				SubJobs b=subJobsList.get(index);
				double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
				double calculatedStartTime=0;
				double possibleStartTime=0;
				if(b.isMedicalCentre() ) {
					if(b.getTotalPeople()<0) {
						calculatedStartTime=(a.getDepartureTime()+tv+b.getloadUnloadRegistrationTime());
						possibleStartTime=Math.max(b.getSoftEndTime(), calculatedStartTime);
					}
					else {
						calculatedStartTime=(a.getDepartureTime()+tv);
						possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);

					}
				}
				else {
					calculatedStartTime=(a.getDepartureTime()+tv);
					possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);
				}

				b.setStartServiceTime(possibleStartTime);
				b.setarrivalTime(a.getDepartureTime()+tv);
				b.setEndServiceTime(b.getstartServiceTime()+b.getReqTime());
				if(b.isClient()) {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimeHomeCareStaff());
				}
				else {
					b.setdepartureTime(b.getArrivalTime()+test.getloadTimePatient());
				}
				p.getListSubJobs().add(b);
				p.getDirectorySubjobs().put(b.getSubJobKey(), b);
			}


			//updatingTimes(p.getListSubJobs());
		}
		else { // iterating over Route
			//	ArrayList<SubJobs> sequence= insertingJob(p,subJobsList,dropoffpatientMedicalCentre2,dropoffHomeCareStaff2,pickpatientMedicalCentre2,pickupHomeCareStaff2, hardConstraints);
			ArrayList<SubJobs> sequence= insertingJobWithoutChangingTime(p,subJobsList,dropoffpatientMedicalCentre2,dropoffHomeCareStaff2,pickpatientMedicalCentre2,pickupHomeCareStaff2, hardConstraints);
			if(!sequence.isEmpty()) {
				inserted=false;
				boolean capTW=testing(sequence); // tw and cap
				boolean deourFeasible=checkingInsertionRespectoToDetour(sequence,subJobsList);		// detour
				if(capTW && deourFeasible) {
					inserted=true;
				}

				if(inserted) {

					SettingTimesSequence(sequence);

					//


					p.getListSubJobs().clear();
					p.getDirectorySubjobs().clear();
					for(SubJobs jobsInRoute:sequence) {
						p.getListSubJobs().add(jobsInRoute);
						p.getDirectorySubjobs().put(jobsInRoute.getSubJobKey(), jobsInRoute);
					}
				}
			}			
		}
		//	checkingWaitingTimes(p);

		return inserted;
	}

	private ArrayList<SubJobs> insertingJobWithoutChangingTime(Parts p, ArrayList<SubJobs> subJobsList2,
			HashMap<String, Couple> dropoffpatientMedicalCentre2, HashMap<String, Couple> dropoffHomeCareStaff2,
			HashMap<String, Couple> pickpatientMedicalCentre2, HashMap<String, Couple> pickupHomeCareStaff2,
			boolean hardConstraints) {

		ArrayList<SubJobs> hardConstraintsJobs = new ArrayList<SubJobs>();
		ArrayList<SubJobs> estimatedTimes= new ArrayList<SubJobs>();
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();

		if(hardConstraints) {
			hardConstraintsJobs = jobsHardTimeWindow(p,subJobsList2); // selection of jobs con hard time window
			list = integratingJobs(hardConstraintsJobs); // se insertan
			estimatedTimes= jobsEstimatedTimeWindow(subJobsList2,p,dropoffpatientMedicalCentre2,dropoffHomeCareStaff2,pickpatientMedicalCentre2,pickupHomeCareStaff2);
			estimatedTimes.sort(Jobs.SORT_BY_STARTW);
		}
		else {
			for(SubJobs j:subJobsList2 ) {
				estimatedTimes.add(j);
			}
			for(SubJobs j:p.getListSubJobs()) {
				list.add(j);
			}
			estimatedTimes.sort(Jobs.SORT_BY_STARTW);
		}
		if(!estimatedTimes.isEmpty() && !list.isEmpty() && hardConstraints) {

			for(SubJobs r: estimatedTimes) {
				boolean inserted=false;
				for(int i=0;i<list.size();i++) { //	r --- j
					SubJobs j=list.get(i);
					ArrayList<SubJobs> copy= new ArrayList<SubJobs>();

					for(int aux=0;aux<i;aux++) {
						copy.add(list.get(aux));
					}
					copy.add(r);
					copy.add(list.get(i));
					inserted= testingCheckingDetourFromDepot(copy,subJobsList2);
					if(inserted) {
						int index=list.indexOf(j);
						list.add(index,r);
						inserted=true;
						break;
					}
				}
				if(!inserted) {	//  j --- r
					ArrayList<SubJobs> copy= new ArrayList<SubJobs>();

					for(int aux=0;aux<list.size();aux++) {
						copy.add(list.get(aux));
					}
					copy.add(r);

					inserted= testing(copy);
					if(inserted) {					
						list.add(r);
						inserted=true;
					}	}
				if(!inserted) {
					list.clear();
					break;
				}
			}
		}
		if(!hardConstraints) {
			boolean partI=feasibleInsertion(estimatedTimes,list);

			System.out.print("System out print");
			System.out.print(p.toString());
			if(!partI) {
				list.clear();
			}

		}
		else {
			if(!estimatedTimes.isEmpty() && hardConstraintsJobs.isEmpty() && hardConstraints) {
				boolean inserted=testing(estimatedTimes);
				if(inserted) {
					SettingTimesSequence(estimatedTimes);
					for(SubJobs j:estimatedTimes) {
						list.add(j);
					}
				}
			}

		}
		return list;
	}


	private boolean testingCheckingDetourFromDepot(ArrayList<SubJobs> sequence, ArrayList<SubJobs> subJobsList2) {
		boolean feasible = false;
		if(vehicleCapacityPart(sequence)) {
			double departureTime=sequence.get(0).getDepartureTime();
			for(int index=1;index<sequence.size();index++) {
				feasible=false;
				SubJobs a=sequence.get(index-1);
				SubJobs b=sequence.get(index);
				double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);			
				double calculatedStartTime=0;
				double possibleStartTime=0;

				// valores definitivos
				double startServiceTime=0;
				double arrivalTime=0;
				double additionalTime=0;

				if(b.isMedicalCentre() ) {
					if(b.getTotalPeople()<0) {
						calculatedStartTime=(departureTime+tv+test.getRegistrationTime());
						possibleStartTime=Math.max(b.getSoftEndTime(), calculatedStartTime);
						additionalTime=test.getRegistrationTime();
					}
					else {
						calculatedStartTime=(departureTime+tv);
						possibleStartTime=Math.max(b.getStartTime(), calculatedStartTime);
					}
				}
				else {
					if(b.isClient()) {
						if(b.getTotalPeople()<0) {// drop off
							calculatedStartTime=(departureTime+tv+test.getloadTimeHomeCareStaff());
							additionalTime=test.getloadTimeHomeCareStaff();
						}
						else { // pick up
							calculatedStartTime=(departureTime+tv);
						}
					}
					else {
						calculatedStartTime=(departureTime+tv+test.getloadTimePatient());
						additionalTime=test.getloadTimePatient();
					}
					possibleStartTime=Math.max(b.getSoftStartTime(), calculatedStartTime);
				}

				startServiceTime=possibleStartTime;
				arrivalTime=departureTime+tv;
				additionalTime=determineAdditionalTime(b);
				if(startServiceTime-(arrivalTime+additionalTime)<=test.getCumulativeWaitingTime()) {
					if(b.isClient()) {
						if(b.getTotalPeople()<0) {
							departureTime=arrivalTime+test.getloadTimeHomeCareStaff();
						}
						else {
							departureTime=arrivalTime+test.getloadTimeHomeCareStaff();
						}
					}
					else {
						if(b.getTotalPeople()<0) {
							departureTime=arrivalTime+test.getloadTimePatient();
						}
						else {
							departureTime=arrivalTime+test.getloadTimePatient();
						}
					}

					// validation TW
					if(b.isMedicalCentre() || b.isPatient()) {
						boolean logic=sequenceLogic(a,b);
						if(arrivalTime>=(b.getStartTime()) && arrivalTime<=b.getEndTime() && logic) {
							feasible=true;
						}
						if(!feasible) {
							break;	
						}
					}
					if(b.isClient() ) {
						boolean logic=sequenceLogicClient(a,b);
						if(arrivalTime>=(b.getSoftStartTime()) && startServiceTime<=b.getSoftEndTime() && logic) {
							feasible=true;
						}
						if(!feasible) {
							break;	
						}

					}
				}
				else {
					break;
				}
			}
		}

		// detour <- con respecto al depot

		return feasible;
	}

	private ArrayList<Edge> selectionEdgesDetourFromDepot(Solution currentSol) {
		ArrayList<Edge> edgesList= new ArrayList<Edge>();
		for(Route r:currentSol.getShift().getRoutes()) {
			for(Edge e:r.getEdges().values()) {
				if(e.gettravelTimeInRoute()>e.getDetour()) {

					edgesList.add(e);

				}
			}
		}
		return edgesList;
	}

	private Solution detourImprovement(int iteration, Solution currentSol) {
		Solution newSol= null;
		// seleccionar detours
		//System.out.println("Sol");
		//System.out.println(currentSol.toString());
		ArrayList<Edge> edgesList= selectionEdgesDetour(currentSol);
		HashMap<String, SubJobs> list= new HashMap<>();

		for(Edge e: edgesList) {
			list.put(e.getOrigin().getSubJobKey(), e.getOrigin());
			list.put(e.getEnd().getSubJobKey(), e.getEnd());
		}

		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs> ();
		for(SubJobs j: list.values()) {
			subJobsList.add(j);
		}



		HashMap<String, Couple> dropoffHomeCareStaff= selectingHomeCareStaffCouple(currentSol);// hard time windows list of home care staff 
		HashMap<String, Couple> pickupHomeCareStaff= selectingHomeCareStaffPickUpCouple(dropoffHomeCareStaff,currentSol);
		HashMap<String, Couple> dropoffpatientMedicalCentre= selectingCoupleDropOffMedical(currentSol);// hard time windows list of patient
		HashMap<String, Couple> pickpatientMedicalCentre= selectingCouplepickpatientMedicalCentre(dropoffpatientMedicalCentre,currentSol);// soft time windows list of patient


		ArrayList<Route> routeList= selectRouteCurrentSolution(currentSol,subJobsList);
		ArrayList<Route> routeToChange=selectRouteRelatedTransportRequest(routeList); // seleccionar rutas de la solución actual
		ArrayList<Route> changedRoute=removingTransportRequestfromCurrentSolution(routeToChange,subJobsList);// eliminar los transport requests
		ArrayList<Route> remainingRoute=selectingRemainingRoute(routeList,routeToChange);
		ArrayList<Parts> parts=selectingCurrentParts(remainingRoute,changedRoute);



		boolean feasibleStaff=false;
		do {
			int iter=-1;
			int amountVehicles=Integer.MAX_VALUE;
			while(inp.getVehicles().get(0).getQuantity()<amountVehicles) {

				if(iter<10) {

					newSol= new Solution();
					iter++;
					if(iter>100) {
						System.out.print(iter);
					}
					HashMap<String,SubJobs> toInsert= new HashMap<String,SubJobs>();
					ArrayList<SubJobs> insertionOrder=sortingJobsList(subJobsList);

					ArrayList<Parts> poolParts= new ArrayList<Parts>();
					for(Parts p: parts) {
						poolParts.add(new Parts(p));
					}
					// pool of routes

					ArrayList<Route> poolRoutes=insertingDepotConnections(parts);
					Solution ux=new Solution();
					for(Route r: poolRoutes) {
						ux.getRoutes().add(r);
					}
					ArrayList<Jobs> missingJobs= checkingMissingJobs(ux);
					if(missingJobs.isEmpty()) {
						//System.out.println("Sol");
					}


					////////
					newSol= constructionSol(insertionOrder,poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,pickupHomeCareStaff,toInsert);
					amountVehicles=newSol.getRoutes().size();

					for(Parts p:poolParts) {
						if(p.getDirectorySubjobs().containsKey("D41")) {
							//System.out.println("Sol");
						}
						if(p.getDirectorySubjobs().containsKey("P41")) {
							//System.out.println("Sol");
						}
					}

				}
				else {break;}
			}
			ArrayList<Jobs> missingJobs= checkingMissingJobs(newSol);
			//System.out.println(newSol.toString());
			if(missingJobs.isEmpty()) {

				Solution shift= shiftDefinition(iteration,newSol);
				shift.setShift(shift);
				newSol.setShift(shift);
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, shift);
				//System.out.println(newSol.toString());
				boolean goodSolution=solutionValitadion(shift);
				goodSolution=solutionValitadion(newSol);
				feasibleStaff=checkingFeasibility(newSol);
				if(!feasibleStaff) {
					newSol= new Solution(currentSol);
					newSol.setShift(currentSol.getShift());
					newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
				}}
			else {
				//System.out.println(newSol.toString());
				newSol= new Solution(currentSol);
				newSol.setShift(currentSol.getShift());
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
			}
			feasibleStaff=checkingFeasibility(newSol);

		}while(!feasibleStaff);


		boolean goodSolution=solutionValitadion(newSol);
		return newSol;
	}

	private Solution updateShift(Solution shift, Solution currentSol) {
		Solution sol= new Solution();
		ArrayList<Parts> shifts= new ArrayList<Parts>();
		for(Route r:shift.getRoutes()) {
			ArrayList<SubJobs> list = new ArrayList<SubJobs>();
			for(SubJobs j:r.getSubJobsList()) {
				Route rInSol=selectionRoute(j,currentSol);
				SubJobs inRoute=rInSol.getJobsDirectory().get(j.getSubJobKey());
				list.add(inRoute);
			}
			Parts p= new Parts();
			p.setListSubJobs(list, inp, test);
			shifts.add(p);
		}
		ArrayList<Route> route= insertingDepotConnections(shifts);
		for(Route newR: route) {
			sol.getRoutes().add(newR);
		}
		return sol;
	}

	private HashMap<Integer, Route> swapingNodesIntraRoute(ArrayList<Edge> edgesList,
			HashMap<Integer, Route> routeList, Solution newSol) {
		HashMap<Integer,Route> newrouteList = new HashMap<>();
		Solution copy= new Solution(newSol);
		Route newRoute= new Route() ;
		for(Edge e: edgesList) {// no los que contengan el depot
			Route r=null;
			if(e.getOrigin().getId()!=1) {
				r=selectionRoute(e.getOrigin(),copy);	
			}
			else {
				if(e.getEnd().getId()!=1) {
					r=selectionRoute(e.getEnd(),copy);	
				}	
			}
			if(r!=null) {
				newRoute=treatment(r,e);
				copy.getRoutes().remove(r);
				copy.getRoutes().add(newRoute);
			}
		}
		for(Route r:copy.getRoutes()) {
			newrouteList.put(r.getIdRoute(), r);
		}

		return newrouteList;
	}

	private Route treatment(Route r, Edge e) {
		Route newRoute= new Route() ;
		ArrayList<SubJobs> sequence = new ArrayList<SubJobs>();
		boolean moved=false;
		boolean last=false;
		for(int i=0;i<r.getSubJobsList().size()-1;i++) {
			SubJobs j=new SubJobs(r.getSubJobsList().get(i));
			if(j.getSubJobKey().equals("P52")) {
				//System.out.println(j.toString());
			}
			SubJobs k=new SubJobs(r.getSubJobsList().get(i+1));
			if(k.getSubJobKey().equals("P52")) {
				//System.out.println(k.toString());
			}
			if(j.getSubJobKey().equals(e.getOrigin().getSubJobKey())) {
				if(e.getOrigin().isPatient()) {
					ArrayList<SubJobs> posibleSeq = new ArrayList<SubJobs>();
					for(SubJobs sb: sequence) {
						posibleSeq.add(sb);
					}

					posibleSeq.add(k);
					posibleSeq.add(j);
					boolean posible=testing(posibleSeq);
					if(posible) {
						moved=true;
						sequence.add(k);
						sequence.add(j);
						if(i+1==r.getSubJobsList().size()-1) {
							last=true;
						}
						i++;
					}
					else {
						sequence.add(j);
					}	
				}

			}
			else {
				if(k.getSubJobKey().equals(e.getEnd().getSubJobKey())) {
					if(!moved) {
						ArrayList<SubJobs> posibleSeq = new ArrayList<SubJobs>();
						for(SubJobs sb: sequence) {
							posibleSeq.add(sb);
						}
						posibleSeq.add(k);
						posibleSeq.add(j);
						boolean posible=testing(posibleSeq);
						if(posible) {
							moved=true;
							sequence.add(k);
							sequence.add(j);
							if(i+1==r.getSubJobsList().size()-1) {
								last=true;
							}
							i++;
						}
						else {
							sequence.add(j);
						}
					}
				}
				else {
					sequence.add(j);}
			}
		}
		if(!last) {
			sequence.add(r.getSubJobsList().get(r.getSubJobsList().size()-1));
		}
		boolean posible=testing(sequence);
		if(posible) {
			SettingTimesSequence(sequence);
			Parts p= new Parts();
			for(SubJobs jobsInRoute:sequence) {
				p.getListSubJobs().add(jobsInRoute);
				p.getDirectorySubjobs().put(jobsInRoute.getSubJobKey(), jobsInRoute);
			}
			newRoute.getPartsRoute().add(p);
			newRoute.updateRouteFromParts(inp, test, jobsInWalkingRoute);
		}
		else {
			newRoute=new Route (r);

		}
		return newRoute;
	}



	private ArrayList<Edge> selectionEdgesDetour(Solution newSol) {
		ArrayList<Edge> edgesList= new ArrayList<Edge>();

		for(Route r:newSol.getShift().getRoutes()) {
			for(Edge e:r.getEdges().values()) {
				if(e.gettravelTimeInRoute()>e.getDetour()) {
					if(e.getOrigin().getId()!=1 && e.getEnd().getId()!=1) {
						edgesList.add(e);
					}
				}
			}
		}
		return edgesList;
	}



	private Solution constructionSol(ArrayList<SubJobs> listSubJobs, ArrayList<Parts> partslistRemaing, Solution sol) {
		Solution newSol=new Solution();
		HashMap<String, Couple> dropoffHomeCareStaff= selectingHomeCareStaffCouple(sol);// hard time windows list of home care staff 
		HashMap<String, Couple> pickupHomeCareStaff= selectingHomeCareStaffPickUpCouple(dropoffHomeCareStaff,sol);
		HashMap<String, Couple> dropoffpatientMedicalCentre= selectingCoupleDropOffMedical(sol);// hard time windows list of patient
		HashMap<String, Couple> pickpatientMedicalCentre= selectingCouplepickpatientMedicalCentre(dropoffpatientMedicalCentre,sol);// soft time windows list of patient
		////

		int amountVehicles=Integer.MAX_VALUE;
		boolean feasibleStaff=false;
		do {

			newSol= new Solution();
			int iter=-1;
			while(inp.getVehicles().get(0).getQuantity()<amountVehicles) {
				if(iter<10) {
					newSol= new Solution();
					iter++;
					if(iter>100) {
						System.out.print(iter);
					}
					HashMap<String,SubJobs> toInsert= new HashMap<String,SubJobs>();
					ArrayList<SubJobs> insertionOrder=sortingJobsList(listSubJobs);

					ArrayList<Parts> poolParts= new ArrayList<Parts>();
					for(Parts p: partslistRemaing) {
						poolParts.add(new Parts(p));
					}
					// pool of routes

					newSol= constructionSol(insertionOrder,poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,pickupHomeCareStaff,toInsert);
					amountVehicles=newSol.getRoutes().size();

				}
				else {break;}
			}
			ArrayList<Jobs> missingJobs= checkingMissingJobs(newSol);
			if(missingJobs.isEmpty()) {

				Solution shift= shiftDefinition(0,newSol);
				newSol.setShift(shift);
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, shift);
				//System.out.println(newSol.toString());
				boolean goodSolution=solutionValitadion(newSol);
				feasibleStaff=checkingFeasibility(newSol);
				if(!feasibleStaff) {
					newSol= new Solution(sol);
					newSol.setShift(sol.getShift());
					newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
				}}
			else {
				newSol= new Solution(sol);
				newSol.setShift(sol.getShift());
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
			}
			feasibleStaff=checkingFeasibility(newSol);

		}while(!feasibleStaff);

		boolean goodSolution=solutionValitadion(newSol);


		return newSol;
	}

	private ArrayList<Parts> remaingParts(ArrayList<Parts> partList, ArrayList<SubJobs> listSubJobs) {
		ArrayList<Parts> partslistRemaing= new ArrayList<Parts>();
		HashMap<String,SubJobs> list= new HashMap<String,SubJobs>();
		for(SubJobs j: listSubJobs) {
			list.put(j.getSubJobKey(), j);
		}

		for(Parts p: partList) {
			partslistRemaing.add(p);
		}
		int i=0;
		for(Parts p:partslistRemaing) {
			boolean remove=true;
			do {
				remove=false;
				for(SubJobs j:p.getListSubJobs()) {
					if(list.containsKey(j.getSubJobKey())) {
						remove=true;
						i++;
						p.getListSubJobs().remove(j);
						p.getDirectorySubjobs().remove(j.getSubJobKey());
						break;
					}
				}

			}while(remove);
		}	
		return partslistRemaing;
	}



	private Route insertingTransportRequest(Route route, ArrayList<SubJobs> estimatedTimes) {
		Route newRoute=null;

		for(Parts p:route.getPartsRoute() ) {
			if(!p.getListSubJobs().isEmpty()) {
				if(p.getListSubJobs().get(0).getId()!=1) {
					boolean inserted=false;
					ArrayList<SubJobs>list=new ArrayList<SubJobs>();
					for(SubJobs j:p.getListSubJobs()) {
						list.add(j);
					}
					if(!list.isEmpty()) {
						inserted=chekingPossibleInsertion(estimatedTimes,list);
						if(inserted) {
							inserted=testing(list);
							if(inserted) {
								SettingTimesSequence(list);
							}
						}

						//				updatingTimes(sequence);
						//				inserted=waintingTime(sequence);

						if(inserted==true) {
							p.getDirectorySubjobs().clear();
							p.getListSubJobs().clear();

							for(SubJobs j: list) {
								p.getListSubJobs().add(j);
								p.getDirectorySubjobs().put(j.getSubJobKey(), j);
								//System.out.println(p.toString());
							}
							break;
						}
					}
				}
			}
		}
		// list <- list of nodes base
		// estimatedTimes<- to insert




		return newRoute;
	}

	private boolean chekingPossibleInsertion(ArrayList<SubJobs> estimatedTimes, ArrayList<SubJobs> base) {
		boolean feasible=false;
		ArrayList<SubJobs> list =new ArrayList<SubJobs>(); 

		for(SubJobs j: base) {
			list.add(j);
		}

		if(!estimatedTimes.isEmpty() && !list.isEmpty()) {

			for(SubJobs r: estimatedTimes) {
				boolean inserted=false;
				for(int i=0;i<list.size();i++) { //	r --- j
					SubJobs j=list.get(i);
					//					double tv=inp.getCarCost().getCost(r.getId()-1, j.getId()-1);
					//					if(j.getArrivalTime()-tv<=r.getEndTime() && j.getArrivalTime()-tv>=r.getStartTime()) {
					//						int index=list.indexOf(j);
					//						list.add(index,r);
					//						inserted=true;
					//						break;
					//					}
					ArrayList<SubJobs> copy= new ArrayList<SubJobs>();

					for(int aux=0;aux<i;aux++) {
						copy.add(list.get(aux));
					}
					copy.add(r);
					copy.add(list.get(i));
					inserted= testing(copy);
					if(inserted) {
						int index=list.indexOf(j);
						list.add(index,r);
						inserted=true;
						break;
					}
				}
				if(!inserted) {	//  j --- r
					ArrayList<SubJobs> copy= new ArrayList<SubJobs>();

					for(int aux=0;aux<list.size();aux++) {
						copy.add(list.get(aux));
					}
					copy.add(r);

					inserted= testing(copy);
					if(inserted) {					
						list.add(r);
						inserted=true;
					}	}
				if(!inserted) {
					list.clear();
					break;
				}
			}
		}
		else {
			if(!estimatedTimes.isEmpty()) {
				boolean inserted=testing(estimatedTimes);
				if(inserted) {
					SettingTimesSequence(estimatedTimes);
					for(SubJobs j:estimatedTimes) {
						list.add(j);
					}
				}
			}

		}
		if(!list.isEmpty()) {
			feasible=true;
			base.clear();
			for(SubJobs j:list) {
				base.add(j);
			}
		}

		return feasible;
	}




	private Solution movement(int iteration, Solution baseSol) {
		Solution currentSol= new Solution(baseSol);
		boolean goodSolution=solutionValitadion(currentSol);
		if(!goodSolution) {
			//System.out.println(currentSol.toString());
		}
		//System.out.println(currentSol.toString());
		ArrayList<Jobs> missingJobs= checkingMissingJobs(currentSol);
		if(!missingJobs.isEmpty()) {
			//System.out.println("Stop");
		}
		HashMap<String, Couple> dropoffHomeCareStaff= selectingHomeCareStaffCouple(currentSol);// hard time windows list of home care staff 
		HashMap<String, Couple> pickupHomeCareStaff= selectingHomeCareStaffPickUpCouple(dropoffHomeCareStaff,currentSol);
		HashMap<String, Couple> dropoffpatientMedicalCentre= selectingCoupleDropOffMedical(currentSol);// hard time windows list of patient
		HashMap<String, Couple> pickpatientMedicalCentre= selectingCouplepickpatientMedicalCentre(dropoffpatientMedicalCentre,currentSol);// soft time windows list of patient
		Solution newSol= null;
		//for(int i=0;i<5;i++) {
		boolean feasibleStaff=false;
		newSol= new Solution();

		do {

			goodSolution=solutionValitadion(currentSol);
			if(!goodSolution) {
				//System.out.println(currentSol.toString());
			}

			newSol= new Solution();
			int iter=-1;

			//SubJobs transpRequest=selectRandomTransportRequest(currentSol.getRoutes().get(randomRoute)); // seleccionar un transport request ! ojo si es un request de paramedico o es un request de un home care staff
			//  1    2   9  11
			Route randomRoute=selectingRoute(currentSol);  
			ArrayList<SubJobs> completeTransportRequest= selectionTransportRequest(iteration,randomRoute,currentSol);// extraer pick up and delivery, en caso de que sea un paciente
			//System.out.println("Solution test");
			//System.out.println(currentSol.toString());
			ArrayList<Route> routeList= selectRouteCurrentSolution(currentSol,completeTransportRequest);
			ArrayList<Route> routeToChange=selectRouteRelatedTransportRequest(routeList); // seleccionar rutas de la solución actual
			ArrayList<Route> changedRoute=removingTransportRequestfromCurrentSolution(routeToChange,completeTransportRequest);// eliminar los transport requests
			ArrayList<Route> remainingRoute=selectingRemainingRoute(routeList,routeToChange);
			ArrayList<Parts> parts=selectingCurrentParts(remainingRoute,changedRoute);
			/// partes
			int amountVehicles=Integer.MAX_VALUE;


			while(inp.getVehicles().get(0).getQuantity()<amountVehicles) {
				if(iter<10) {
					newSol= new Solution();
					iter++;
					if(iter>100) {
						System.out.print(iter);
					}
					HashMap<String,SubJobs> toInsert= new HashMap<String,SubJobs>();
					ArrayList<SubJobs> insertionOrder=sortingJobsList(completeTransportRequest);

					ArrayList<Parts> poolParts= new ArrayList<Parts>();
					for(Parts p: parts) {
						poolParts.add(new Parts(p));
					}
					// pool of routes

					newSol= constructionSol(insertionOrder,poolParts,dropoffHomeCareStaff,dropoffpatientMedicalCentre,pickpatientMedicalCentre,pickupHomeCareStaff,toInsert);
					amountVehicles=newSol.getRoutes().size();

				}
				else {break;}
			}
			missingJobs= checkingMissingJobs(newSol);
			if(missingJobs.isEmpty()) {

				Solution shift= shiftDefinition(iteration,newSol);
				shift.setShift(shift);
				newSol.setShift(shift);
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, shift);
				//System.out.println(newSol.toString());
				goodSolution=solutionValitadion(shift);
				goodSolution=solutionValitadion(newSol);
				feasibleStaff=checkingFeasibility(newSol);
				if(!feasibleStaff) {
					newSol= new Solution(baseSol);
					newSol.setShift(baseSol.getShift());
					newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
				}}
			else {
				//System.out.println(newSol.toString());
				newSol= new Solution(baseSol);
				newSol.setShift(baseSol.getShift());
				newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
			}
			feasibleStaff=checkingFeasibility(newSol);

		}while(!feasibleStaff);

		if(newSol.getobjectiveFunction()<currentSol.getobjectiveFunction()) {
			currentSol= new Solution(newSol);	
		}
		else {
			currentSol= new Solution(baseSol);
		}

		//}
		goodSolution=solutionValitadion(newSol);
		return newSol;
	}


	private Route selectingRoute(Solution currentSol) {

		ArrayList<Route> RouteList= new ArrayList<Route> ();

		for(Route s:currentSol.getRoutes()) { // selecting routes which contains nodes with hard time window
			for(SubJobs j:s.getSubJobsList()) {
				if(j.getTotalPeople()<0) {
					if(j.isClient() || j.isMedicalCentre()) {
						RouteList.add(s)	;
						break;
					}
				}
			}
		}

		int randomRoute=rn.nextInt(RouteList.size());
		Route r=RouteList.get(randomRoute);
		return r;
	}

	private ArrayList<Route> selectingRemainingRoute(ArrayList<Route> routeList, ArrayList<Route> routeToChange) {
		ArrayList<Route> remainingRoute= new ArrayList<Route>();
		for(Route j: routeList) {
			if(!routeToChange.contains(j)) {
				remainingRoute.add(j);
			}
			else {
				System.out.print("Ok");
			}
		}
		return remainingRoute;
	}

	private ArrayList<Parts> selectingCurrentParts(ArrayList<Route> routeList, ArrayList<Route> changedRoute) {
		ArrayList<Parts> poolParts= new ArrayList<Parts>();
		ArrayList<Parts> parts= new ArrayList<Parts>();

		for(Route r:routeList) {
			Parts newPart= new Parts();
			ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>();
			for(Parts p:r.getPartsRoute()) {
				if(p.getListSubJobs().get(0).getSubJobKey().equals("P72")) {
					//System.out.println("Stop");
				}
				if(p.getListSubJobs().get(0).getSubJobKey().equals("P1")) {
					newPart= new Parts();
					listSubJobs= new ArrayList<SubJobs>();
				}
				else {
					if(p.getListSubJobs().get(0).getSubJobKey().equals("D1")) {
						newPart.setListSubJobs(listSubJobs, inp, test);
						poolParts.add(newPart);
					}
					else{
						for(SubJobs j: p.getListSubJobs()) {
							listSubJobs.add(j);
						}
					}
				}
			}
		}
		for(Route r:changedRoute) {
			boolean emptyParts=true;
			while(emptyParts) {
				for(int p=0;p<r.getPartsRoute().size();p++) {
					if(r.getPartsRoute().get(p).getListSubJobs().isEmpty()) {
						r.getPartsRoute().remove(p);
						break;
					}
					if(p==r.getPartsRoute().size()-1) {
						emptyParts=false;
					}
				}
			}
		}
		for(Route r:changedRoute) {
			Parts newPart= new Parts();
			ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>();
			for(Parts p:r.getPartsRoute()) {
				if(p.getListSubJobs().size()==0) {
					//System.out.println("Stop");
				}
				if(p.getListSubJobs().get(0).getSubJobKey().equals("P1")) {
					newPart= new Parts();
					listSubJobs= new ArrayList<SubJobs>();
				}
				else {
					if(p.getListSubJobs().get(0).getSubJobKey().equals("D1")) {
						newPart.setListSubJobs(listSubJobs, inp, test);
						poolParts.add(newPart);
					}
					else{
						for(SubJobs j: p.getListSubJobs()) {
							listSubJobs.add(j);
						}
					}
				}
			}
		}
		for(Parts p: poolParts) {
			if(!p.getListSubJobs().isEmpty()) {
				parts.add(p);
			}
		}
		return parts;
	}

	private ArrayList<Route> removingTransportRequestfromCurrentSolution(ArrayList<Route> routeToChange, ArrayList<SubJobs> completeTransportRequest) {
		ArrayList<Route> changedRoute= new ArrayList<Route>();
		for(Route r: routeToChange) {
			Route r2=removingTransportRequest(r,completeTransportRequest);
			changedRoute.add(r2);
		}

		return changedRoute;
	}

	private Route removingTransportRequest(Route ref, ArrayList<SubJobs> completeTransportRequest) {

		Route r=new Route(ref);
		for(SubJobs j: completeTransportRequest) {
			if(r.getJobsDirectory().containsKey(j.getSubJobKey())) {
				for(Parts p:r.getPartsRoute()) {
					if(p.getDirectorySubjobs().containsKey("71")) {
						int a=-1;
					}
					if(p.getDirectorySubjobs().containsKey(j.getSubJobKey())) {
						p.getDirectorySubjobs().remove(j.getSubJobKey());
						int index=-1;
						for(SubJobs x:p.getListSubJobs()) {
							if(x.getSubJobKey().equals(j.getSubJobKey())) {
								index=p.getListSubJobs().indexOf(x);	
								break;
							}
						}
						p.getListSubJobs().remove(p.getListSubJobs().get(index));

					}
				}
				//r.updateRouteFromParts(inp, test, jobsInWalkingRoute);
			}
		}
		// 1) Eliminar las partes que esten vacias
		boolean remove = false;
		do {
			for(Parts p:r.getPartsRoute()) {
				remove = false;
				if(p.getListSubJobs().isEmpty()) {
					r.getPartsRoute().remove(p);
					remove = true;
				}
				if(remove) {
					break;
				}
			}
		}
		while(remove);

		//2) actualizar la lista de trabajos

		r.getSubJobsList().clear();
		r.getJobsDirectory().clear();
		for(Parts p:r.getPartsRoute()) {
			if(p.getListSubJobs().get(0).getId()!=1) {
				for(SubJobs j: p.getListSubJobs()) {
					r.getSubJobsList().add(j);
					r.getJobsDirectory().put(j.getSubJobKey(),j);
				}
			}

		}

		return r;
	}

	private ArrayList<Route> selectRouteRelatedTransportRequest(ArrayList<Route> routeList2) {
		ArrayList<Route> routeList = new ArrayList<Route>();
		for(Route r: routeList2) {
			for(SubJobs j: r.getSubJobsList()) {
				if(r.getJobsDirectory().containsKey(j.getSubJobKey())) {
					routeList.add(r);
					break;
				}
			}
		}
		return routeList;
	}

	private ArrayList<Route> selectRouteCurrentSolution(Solution currentSol,
			ArrayList<SubJobs> completeTransportRequest) {
		ArrayList<Route> routeList = new ArrayList<Route>();
		for(Route r: currentSol.getRoutes()) {
			routeList.add(r);
		}
		return routeList;
	}

	private ArrayList<SubJobs> selectionTransportRequest(int k, Route r, Solution newSol) {
		//		HashMap<String, Couple> dropoffHomeCareStaff= selectingHomeCareStaffCouple(newSol);// hard time windows list of home care staff 
		//		HashMap<String, Couple> pickupHomeCareStaff= selectingHomeCareStaffPickUpCouple(dropoffHomeCareStaff,newSol);
		//		HashMap<String, Couple> dropoffpatientMedicalCentre= selectingCoupleDropOffMedical(newSol);// hard time windows list of patient
		//		HashMap<String, Couple> pickpatientMedicalCentre= selectingCouplepickpatientMedicalCentre(dropoffpatientMedicalCentre,newSol);// soft time windows list of patient

		ArrayList<SubJobs> allTransportRequest= new ArrayList<SubJobs>();
		do {
			allTransportRequest= selectingTransportRequests(r);  // seleccionar random transport request
		}while(allTransportRequest.isEmpty());
		ArrayList<SubJobs> randomTransportRequest= selectingTransportRequestsToRemove(k,allTransportRequest);
		ArrayList<SubJobs> listSubJobs= transportRequestToAssign(randomTransportRequest);

		return listSubJobs;
	}


	private ArrayList<SubJobs> transportRequestToAssign(ArrayList<SubJobs> allTransportRequest) {
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>(); 
		for(SubJobs transpRequest: allTransportRequest) {
			if(transpRequest.isPatient() || transpRequest.isMedicalCentre()) {// seleccionar transport request
				Couple c1= selectingDropOffMCCouple(transpRequest,dropoffpatientMedicalCentre);
				Couple c2=selectingPickUpMCCouple(transpRequest,pickpatientMedicalCentre);
				subJobsList.add(new SubJobs(c1.getPresent()));
				subJobsList.add(new SubJobs(c1.getFuture()));
				subJobsList.add(new SubJobs(c2.getPresent()));
				subJobsList.add(new SubJobs(c2.getFuture()));
			}
			else {
				Couple c1= selectingDropOffMCCoupleHomeCareStaff(transpRequest,dropoffHomeCareStaff);
				subJobsList.add(new SubJobs(c1.getPresent()));
				subJobsList.add(new SubJobs(c1.getFuture()));
			}
		}
		return subJobsList;
	}

	private ArrayList<SubJobs> transportRequest(ArrayList<SubJobs> allTransportRequest) {
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>(); 
		HashMap<String, Jobs> incorporater=new HashMap<String, Jobs>();
		for(SubJobs transpRequest: allTransportRequest) {
			if(transpRequest.isPatient()) {// seleccionar transport request
				Couple c1= selectingDropOffMCCouple(transpRequest,dropoffpatientMedicalCentre);
				Couple c2=selectingPickUpMCCouple(transpRequest,pickpatientMedicalCentre);

				subJobsList.add(new SubJobs(c1.getPresent()));
				subJobsList.add(new SubJobs(c1.getFuture()));
				subJobsList.add(new SubJobs(c2.getPresent()));
				subJobsList.add(new SubJobs(c2.getFuture()));

			}
			else {
				if(transpRequest.isClient()) {
					Couple x= selectingDropOffMCCoupleHomeCareStaff(transpRequest,dropoffHomeCareStaff);
					if(transpRequest.getSubJobKey().equals("P26") && x==null ) {
						System.out.print(transpRequest.toString());
					}
					Couple c1= selectingDropOffMCCoupleHomeCareStaff(transpRequest,dropoffHomeCareStaff);
					subJobsList.add(new SubJobs(c1.getPresent()));
					subJobsList.add(new SubJobs(c1.getFuture()));
				}
			}
		}
		return subJobsList;
	}

	private ArrayList<SubJobs> selectingTransportRequestsToRemove(int k, ArrayList<SubJobs> allTransportRequest) {
		ArrayList<SubJobs> randomransportRequest= new ArrayList<SubJobs>(); 

		if(k<allTransportRequest.size()) {
			while(randomransportRequest.size()<k) {
				int randomRequest= rn.nextInt(allTransportRequest.size());
				SubJobs j=allTransportRequest.get(randomRequest);
				if(!randomransportRequest.contains(j)) {
					randomransportRequest.add(j);}
			}
		}
		else {
			for(SubJobs j:  allTransportRequest) {
				randomransportRequest.add(j);
			}
		}
		return randomransportRequest;
	}

	private ArrayList<SubJobs> selectingTransportRequests(Route r) {
		ArrayList<SubJobs> allTransportRequest= new ArrayList<SubJobs>(); 
		for(SubJobs j:r.getSubJobsList()) {
			if(j.isMedicalCentre() && j.getTotalPeople()<0) {
				allTransportRequest.add(j);
			}
			if(j.isClient() && j.getTotalPeople()<0) {
				allTransportRequest.add(j);	
			}
		}
		return allTransportRequest;
	}

	private Couple selectingDropOffMCCoupleHomeCareStaff(SubJobs transpRequest,
			HashMap<String, Couple> dropoffHomeCareStaff2) {
		String key="";
		Couple c1=null;
		if(transpRequest.getTotalPeople()<0) {
			key=transpRequest.getSubJobKey();
		}
		else {
			key="D"+transpRequest.getId();
		}

		for(Couple c: dropoffHomeCareStaff2.values()) {
			if(c.getPresent().getSubJobKey().equals(transpRequest.getSubJobKey()) || c.getFuture().getSubJobKey().equals(transpRequest.getSubJobKey()) ) {
				c1=new Couple(c,inp,test);
				break;
			}
		}
		return c1;
	}

	private Couple selectingPickUpMCCouple(SubJobs transpRequest, HashMap<String, Couple> pickpatientMedicalCentre2) {
		Couple c1=null;
		if(transpRequest.isPatient()) {
			c1=patientSelection(transpRequest,pickpatientMedicalCentre2);
		}
		else {
			c1=selectingCouplePickUpFromMC(transpRequest,pickpatientMedicalCentre2);

		}
		return c1;
	}

	private Couple patientSelection(SubJobs transpRequest, HashMap<String, Couple> pickpatientMedicalCentre2) {
		String key="";
		Couple c1=null;
		if(transpRequest.isPatient() && transpRequest.getTotalPeople()<0) {// pick Up patient
			key=transpRequest.getSubJobKey();
		}
		else {
			if(transpRequest.isPatient() && transpRequest.getTotalPeople()>0) {// pick Up patient
				key="D"+transpRequest.getId();
			}
		}

		for(Couple c: pickpatientMedicalCentre2.values()) {
			if(c.getPresent().getSubJobKey().equals(key) || c.getFuture().getSubJobKey().equals(key) ) {
				c1=new Couple(c,inp,test);
			}
		}
		return c1;
	}

	private Couple selectingDropOffMCCouple(SubJobs transpRequest,
			HashMap<String, Couple> dropoffpatientMedicalCentre2) {
		String key="";
		Couple c1=null;
		if(transpRequest.isPatient()) {
			c1=selectingCouplePickUp(transpRequest,dropoffpatientMedicalCentre2);
		}
		else { // medicle centre
			c1=selectingCoupleFromMC(transpRequest,dropoffpatientMedicalCentre2);
		}


		return c1;
	}

	private Couple selectingCoupleFromMC(SubJobs transpRequest, HashMap<String, Couple> dropoffpatientMedicalCentre2) {
		String key="";
		Couple c1=null;
		if(transpRequest.getTotalPeople()<0){
			key=transpRequest.getSubJobKey();
		}
		else {
			key="D"+transpRequest.getId()+transpRequest.getIdUser();
		}
		for(Couple c: dropoffpatientMedicalCentre2.values()) {
			if(c.getPresent().getSubJobKey().equals(key) || c.getFuture().getSubJobKey().equals(key) ) {
				c1=new Couple(c,inp,test);
			}
		}

		return c1;
	}

	private Couple selectingCouplePickUpFromMC(SubJobs transpRequest, HashMap<String, Couple> dropoffpatientMedicalCentre2) {
		String key="";
		Couple c1=null;
		if(transpRequest.getTotalPeople()>0){
			key=transpRequest.getSubJobKey();
		}
		else {
			key="P"+transpRequest.getId()+transpRequest.getIdUser();
		}
		for(Couple c: dropoffpatientMedicalCentre2.values()) {
			if(c.getPresent().getSubJobKey().equals(key) || c.getFuture().getSubJobKey().equals(key) ) {
				c1=new Couple(c,inp,test);
			}
		}

		return c1;
	}

	private Couple selectingCouplePickUp(SubJobs transpRequest, HashMap<String, Couple> dropoffpatientMedicalCentre2) {
		String key="";
		Couple c1=null;

		if(transpRequest.isPatient() && transpRequest.getTotalPeople()>0) {// pick Up patient
			key=transpRequest.getSubJobKey();
		}
		else {
			if(transpRequest.isPatient() && transpRequest.getTotalPeople()<0) {// pick Up patient
				key="P"+transpRequest.getId();
			}
		}

		for(Couple c: dropoffpatientMedicalCentre2.values()) {
			if(c.getPresent().getSubJobKey().equals(key) || c.getFuture().getSubJobKey().equals(key) ) {
				c1=new Couple(c,inp,test);
			}
		}
		return c1;
	}





	//	private Solution interRoutesMovements(int iteration, Solution sol) {
	//
	//		Solution currentSolution=new Solution(sol);
	//		ArrayList<Route> transferRoute= new ArrayList<Route>();
	//
	//		////
	//		ArrayList<Route> routeList= new ArrayList<Route>();
	//		for(Route r: sol.getRoutes()) {
	//			routeList.add(r);
	//		}
	//		//boolean goodSolution=validationSequenceRoutes(routeList);
	//		SubJobs future=null;
	//		Route ir=selectionRoute(future,currentSolution);
	//
	//		////
	//		Solution newSolution=new Solution();
	//		//System.out.println("Solution 123" + currentSolution.toString());
	//		int[][] combination= new int[currentSolution.getRoutes().size()][currentSolution.getRoutes().size()];
	//		int totalRoutes=currentSolution.getRoutes().size();
	//
	//		for(int i=0;i<totalRoutes;i++) {
	//			Solution PartialSolution=new Solution();
	//			int indexR=rn.nextInt(currentSolution.getRoutes().size()-1); 
	//			Route RouteR=currentSolution.getRoutes().get(indexR);
	//			boolean checkJObs=readingRoute(RouteR);
	//			if(checkJObs==false) {
	//				//System.out.println(" Index " + RouteR);
	//			}
	//			int indexT=	rn.nextInt(currentSolution.getRoutes().size()-1); 
	//			Route RouteT=currentSolution.getRoutes().get(indexT);
	//			checkJObs=readingRoute(RouteT);
	//			if(checkJObs==false) {
	//				//System.out.println(" Index " + RouteT);
	//			}
	//			if(indexR!=indexT && !transferRoute.contains(RouteT) && !transferRoute.contains(RouteR)) {
	//				if(RouteR.getIdRoute()==9||RouteT.getIdRoute()==9) {
	//					//System.out.println(" index " + indexT + " Index " + indexR);
	//				}
	//				if(RouteR.getJobsDirectory().isEmpty()||RouteT.getJobsDirectory().isEmpty()) {
	//					//System.out.println(" index " + indexT + " Index " + indexR);
	//				}
	//				if(RouteR.getJobsDirectory().containsKey("D11")||RouteT.getJobsDirectory().containsKey("D11")) {
	//					//System.out.println(" index " + RouteT + " Index " + RouteR);
	//				}
	//				PartialSolution.getRoutes().add(new Route(currentSolution.getRoutes().get(indexR)));
	//				PartialSolution.getRoutes().add(new Route(currentSolution.getRoutes().get(indexT)));
	//				PartialSolution.checkingSolution(inp, test, jobsInWalkingRoute, PartialSolution);
	//				Solution newPartialSol=sortInsertionProcedurePartial(PartialSolution);
	//				if(!newPartialSol.getRoutes().isEmpty()) {
	//					transferRoute.add(currentSolution.getRoutes().get(indexR));
	//					transferRoute.add(currentSolution.getRoutes().get(indexT));
	//					for(Route r:newPartialSol.getRoutes()) {
	//						if(!r.getPartsRoute().isEmpty()) {
	//							newSolution.getRoutes().add(r);}
	//					}
	//				}
	//				//else {
	//				//					newSolution.getRoutes().add(currentSolution.getRoutes().get(indexR));
	//				//					newSolution.getRoutes().add(currentSolution.getRoutes().get(indexT));
	//				//				}
	//			}
	//		}
	//
	//		// adding the additional routes
	//		for(Route r: currentSolution.getRoutes()) {
	//			if(!transferRoute.contains(r)) {
	//				newSolution.getRoutes().add(r);
	//			}
	//		}
	//
	//
	//		//System.out.println("Solution " + newSolution.toString());
	//		//System.out.println("Current Solution " + currentSolution.toString());
	//		routeList= new ArrayList<Route>();
	//		for(Route r: newSolution.getRoutes()) {
	//			routeList.add(r);
	//		}
	//		//goodSolution=validationSequenceRoutes(routeList);
	//		Solution shift= shiftDefinition(iteration,newSolution);
	//		newSolution.setShift(shift);
	//		newSolution.checkingSolution(inp, test, jobsInWalkingRoute, newSolution.getShift());
	//		return newSolution;
	//	}



	private Solution reducingVehicles(int iteration, Solution solsorting) {
		Solution sol= new Solution(solsorting);
		Solution newSol= new Solution();
		ArrayList<Route> routesList= new ArrayList<Route>();
		for(Route r: sol.getRoutes()) { // selecting routes con un solo trabajo
			if(r.getSubJobsList().size()==1) {
				routesList.add(r);
			}
		}
		// merging routes with one Jobs
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();
		for(Route r: routesList) {
			for(SubJobs j:r.getSubJobsList()) {
				subJobsList.add(j);
			}
		}
		subJobsList.sort(SubJobs.SORT_BY_STARTW);
		if(!subJobsList.isEmpty()) {
			double departure_a=subJobsList.get(0).getStartTime()+(subJobsList.get(0).getDepartureTime()-subJobsList.get(0).getArrivalTime());
			boolean inserted=false;
			if(vehicleCapacityPart(subJobsList) ) {
				for(int index=1;index<subJobsList.size();index++) {
					SubJobs a=subJobsList.get(index-1);
					SubJobs b=subJobsList.get(index);
					double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
					double test= departure_a+tv+b.getloadUnloadRegistrationTime()+b.getloadUnloadTime();
					if(departure_a+tv+b.getloadUnloadRegistrationTime()+b.getloadUnloadTime()<=b.getEndTime() && departure_a+tv+b.getloadUnloadRegistrationTime()+b.getloadUnloadRegistrationTime()>=b.getStartTime()) {
						departure_a=departure_a+tv+b.getloadUnloadTime();
						inserted=true;
					}
					else {
						inserted=false;
						break;
					}
				}
			}
			if(inserted) {
				departure_a=subJobsList.get(0).getStartTime()+(subJobsList.get(0).getDepartureTime()-subJobsList.get(0).getArrivalTime());
				SubJobs firstJob=subJobsList.get(0);
				firstJob.setarrivalTime(subJobsList.get(0).getStartTime());
				firstJob.setdepartureTime(departure_a);
				for(int index=1;index<subJobsList.size();index++) {
					SubJobs a=subJobsList.get(index-1);
					SubJobs b=subJobsList.get(index);
					double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
					double delta=b.getDepartureTime()-b.getArrivalTime();
					b.setarrivalTime(departure_a+tv);
					b.setdepartureTime(b.getArrivalTime()+delta);
					departure_a=b.getDepartureTime();
				}	
			}
			Parts p= new Parts();
			p.setListSubJobs(subJobsList, inp, test);
			ArrayList<Parts> schift2= new ArrayList<Parts>();
			schift2.add(p);
			ArrayList<Route> poolRoutes=insertingDepotConnections(schift2);
			for(Route r: sol.getRoutes()) { // selecting routes con un solo trabajo
				if(!routesList.contains(r)) {
					poolRoutes.add(r);
				}
			}

			stackingRoutes(poolRoutes);
			for(Route r: poolRoutes) {
				newSol.getRoutes().add(r);
			}



			newSol.setShift(sol.getShift());
			newSol.checkingSolution(inp, test, jobsInWalkingRoute, newSol.getShift());
			//System.out.println("Solution");
			//System.out.println(newSol.toString());


			Solution shift= shiftDefinition(iteration,newSol);
			newSol.setShift(shift);
		}
		else {
			newSol=new Solution (solsorting);
		}

		return newSol;
	}





	private ArrayList<Route> getRoutesDetourViolation(Solution currentSol) {
		ArrayList<Route> violationRoute= new ArrayList<Route>(); 
		for(Route r:currentSol.getRoutes()) {
			if(r.getdetourViolation()>0) {
				violationRoute.add(r);
			}
		}
		if(violationRoute.isEmpty()) {
			for(Route r:currentSol.getRoutes()) {
				//if(r.getdetourViolation()>0) {
				violationRoute.add(r);
				//}
			}
		}

		return violationRoute;
	}

	private Route intraMovement(Route r) {
		Route routeRef=new Route(r); // crear una nueva ruta, se mantiene la actual
		//System.out.println("Routes");
		//System.out.println(r.toString());
		ArrayList<Edge> edgesExcedingDetour=detourViolations(routeRef);
		edgesExcedingDetour.sort(Edge.startServiceTimeOrigenNode);
		ArrayList<SubJobs> subJobsHardTW=selectingRefNodes(routeRef); // ref nodes has a hard time window
		//subJobsHardTW.sort(Jobs.SORT_BY_STARTSERVICETIME);
		subJobsHardTW.sort(Jobs.SORT_BY_STARTW);
		swappingParts(r);
		r.updateRouteFromParts(inp, test, jobsInWalkingRoute);
		// ojo tiene que insertar las conexiones del depot
		return r;
	}

	private void swappingParts(Route r) {
		// se seleccionan los trabajos que estan en los nodos
		Route newRoute=new Route();
		ArrayList<SubJobs> list= null;
		Parts initialPart=new Parts();
		for(Parts p:r.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {
				if(j.getSubJobKey().equals("P1")) { // part
					initialPart=p;
					list= new ArrayList<SubJobs>();
				}
				else {
					if(j.getSubJobKey().equals("D1")) { // the list of jobs 
						Parts intermediateJobs=new Parts();
						checkingNewOrder(list);
						intermediateJobs.setListSubJobs(list, inp, test);
						newRoute.getPartsRoute().add(initialPart);
						newRoute.getPartsRoute().add(intermediateJobs);
						newRoute.getPartsRoute().add(p);
						//System.out.println("Solution");
					}
					else {
						list.add(j);
					}
				}
			}
		}
		if(newRoute!=null) {
			r.getPartsRoute().clear();
			r.getSubJobsList().clear();
			r.getJobsDirectory().clear();
			for(Parts p: newRoute.getPartsRoute()) {
				r.getPartsRoute().add(p);
			}}
		//System.out.println("Route "+newRoute.toString());
	}

	private void checkingNewOrder(ArrayList<SubJobs> list) {
		// sorting nodes according to the distance to the depot
		list.sort(SubJobs.SORT_BY_STARTW);

		HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();

		ArrayList<SubJobs> sequence= new ArrayList<SubJobs>();
		for(int i=0;i<list.size();i++) {
			SubJobs nextSubJobs=selectingNextNodes(list,sequence,subJobsList);	
			sequence.add(nextSubJobs);
			subJobsList.put(nextSubJobs.getSubJobKey(), nextSubJobs);
		}

		//System.out.println("Finish");

	}

	private SubJobs selectingNextNodes(ArrayList<SubJobs> list, ArrayList<SubJobs> sequence, HashMap<String, SubJobs> subJobsList2) {
		list.sort(SubJobs.SORT_BY_STARTW);
		SubJobs jobs= null;
		double minDistance= Double.MAX_VALUE;
		double minTime= Double.MAX_VALUE;
		double tv=0;
		if(sequence.isEmpty()) {
			for(SubJobs j: list) {
				tv=inp.getCarCost().getCost(0, j.getId()-1);
				if(minDistance>tv && minTime>=j.getStartTime()) {
					minDistance=tv;
					jobs=j;
				}
			}

		}
		else {
			for(SubJobs j: list) {
				if(!subJobsList2.containsKey(j.getSubJobKey())) {
					tv=inp.getCarCost().getCost(0, j.getId()-1);
					if(minDistance>tv && minTime>=j.getStartTime()) {
						minDistance=tv;
						jobs=j;
					}
				}
			}
		}
		return jobs;
	}

	private ArrayList<SubJobs> selectingRefNodes(Route routeRef) {
		ArrayList<SubJobs> refNodes = new ArrayList<SubJobs> ();
		for(SubJobs js: routeRef.getSubJobsList()) { // verificar que los trabajos de las partes coincidan con la lista de subjobs 
			if(js.getTotalPeople()<0) {
				if(js.isClient() || js.isMedicalCentre()) {
					refNodes.add(js);
				}
			}
		}
		return refNodes;
	}

	private ArrayList<Edge> detourViolations(Route routeRef) {
		ArrayList<Edge> edgesExcedingDetour=new ArrayList<Edge>();
		for(Edge e:routeRef.getEdges().values()) {
			if(e.gettravelTimeInRoute()>e.getDetour()) {
				if(e.getOrigin().getSubJobKey().equals("P1") && e.getEnd().getSubJobKey().equals("P72")) {
					//System.out.println("Stop");
				}

				edgesExcedingDetour.add(e);
			}
		}
		return edgesExcedingDetour;
	}



	private Route mergingSetRoutes(ArrayList<Route> routeToMerge) {
		routeToMerge.sort(Route.SORT_BY_EarlyJob);
		Route newRoute=null;
		ArrayList<SubJobs> jobsList= new ArrayList<SubJobs>(); 
		for(Route r:routeToMerge) {
			for(int p=1;p<r.getPartsRoute().size()-1;p++) {
				for(SubJobs j:r.getPartsRoute().get(p).getListSubJobs()) {
					jobsList.add(j);
				}
			}
		}

		jobsList.sort(SubJobs.SORT_BY_STARTW);
		ArrayList<SubJobs> sequenceJobs= new ArrayList<SubJobs>(); 
		double paramedics=0;
		double homeCareStaff=0;

		for(Route r:routeToMerge) {
			paramedics+=r.getAmountParamedic();
			homeCareStaff+=r.getHomeCareStaff();
		}

		double CapVehicle=homeCareStaff+paramedics;

		CapVehicle+=jobsList.get(0).getTotalPeople();
		sequenceJobs.add(jobsList.get(0));
		for(int i=1;i<jobsList.size();i++) {
			SubJobs Inode=jobsList.get(i-1);
			SubJobs Jnode=jobsList.get(i);
			double tv=inp.getCarCost().getCost(Inode.getId()-1, Jnode.getId()-1);
			double xxx=Inode.getDepartureTime()+tv;
			//if(Inode.getDepartureTime()+tv<=Jnode.getEndTime()) {
			if(Inode.getDepartureTime()+tv<=Jnode.getEndTime() &&  Inode.getDepartureTime()+tv>=Jnode.getStartTime()) { // time window
				if(CapVehicle+Jnode.getTotalPeople()<inp.getVehicles().get(0).getMaxCapacity()) {
					CapVehicle+=Jnode.getTotalPeople();
					Jnode.setarrivalTime(Inode.getDepartureTime()+tv);
					Jnode.setdepartureTime(Jnode.getArrivalTime()+Jnode.getdeltaArrivalDeparture());
					sequenceJobs.add(Jnode);
				}
				else {
					sequenceJobs.clear();
					break;
				}
			}
			else {
				sequenceJobs.clear();
				break;
			}
		}
		//System.out.println("Stop");
		if(!sequenceJobs.isEmpty()) {
			newRoute=new Route();
			Parts p = new Parts();

			p.setListSubJobs(sequenceJobs, inp, test);
			newRoute.getPartsRoute().add(routeToMerge.get(0).getPartsRoute().get(0));
			newRoute.getPartsRoute().add(p);
			newRoute.getPartsRoute().add(routeToMerge.get(0).getPartsRoute().get(routeToMerge.get(0).getPartsRoute().size()-1));
			newRoute.updateRouteFromParts(inp, test, jobsInWalkingRoute);
			newRoute.setAmountParamedic(paramedics);
			newRoute.setHomeCareStaff(homeCareStaff);
		}
		return newRoute;
	}

	private Solution mergingIndividualsTrip(Solution s) {
		Solution sol1 = new Solution (s);
		Solution sol = new Solution ();
		for(Route r1:sol1.getRoutes()) {
			for(Route r2:sol1.getRoutes()) {
				if(r1.getIdRoute()== 3 && r2.getIdRoute()== 5) {
					//System.out.println("Stop");
				}
				if(r1!=r2 && !r1.getPartsRoute().isEmpty() && !r2.getPartsRoute().isEmpty()) {
					insertingRoute(r1,r2);
				}
			}
		}

		for(Route r:sol1.getRoutes()) {
			if(!r.getPartsRoute().isEmpty()) {
				sol.getRoutes().add(r);
			}
		}
		sol.checkingSolution(inp, test, jobsInWalkingRoute, this.initialSol);
		//System.out.println(sol.toString());

		return sol;
	}

	private void insertingRoute(Route r1, Route r2) {
		Route r= new Route();
		HashMap<String, SubJobs> listJobs= listJobs(r1,r2);
		ArrayList<SubJobs> dropOffJobsPatientsMC= selectiondropOffJobsPatientsMC(r1,r2);  		// selection drop-off patient medical centre
		dropOffJobsPatientsMC.sort(Jobs.SORT_BY_ARRIVALTIME);
		ArrayList<SubJobs> dropOffJobsClient= selectiondropOffJobsHHC(r1,r2); // selection of drop-off home care staff medical centre
		dropOffJobsClient.sort(Jobs.TWSIZE_Early);
		//boolean inserted= insertingSubJobs(r1,r2,r);
		boolean inserted= stackingJobs(r1,r2,r);
		if(inserted) {
			r.getPartsRoute().add(0,r1.getPartsRoute().get(0));
			r.getPartsRoute().add(r1.getPartsRoute().get(r1.getPartsRoute().size()-1));
			r1.getPartsRoute().clear();
			r1.getEdges().clear();
			r1.getSubJobsList().clear();
			r1.getEdges().clear();

			r2.getPartsRoute().clear();
			r2.getEdges().clear();
			r2.getSubJobsList().clear();
			r2.getEdges().clear();
			for(Parts p :r.getPartsRoute()) {
				r1.getPartsRoute().add(p);
				for(Edge e:p.getDirectoryConnections().values()) {
					p.getDirectoryConnections().put(e.getEdgeKey(), e);
					r1.getEdges().put(e.getEdgeKey(), e);
				}
			}
			r1.updateRouteFromParts(inp, test, jobsInWalkingRoute);
		}
	}

	private boolean stackingJobs(Route r1, Route r2, Route r) {
		boolean merge=false;
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		for(SubJobs j:r1.getSubJobsList()) {
			list.add(j);
		}
		for(SubJobs j:r2.getSubJobsList()) {
			list.add(j);
		}
		list.sort(Jobs.SORT_BY_ARRIVALTIME);

		if(vehicleCapacityPart(list)) {
			boolean feasible= evaluatingArrivalTimeChangingTime(list);
			if(feasible) {
				Parts p= new Parts();
				p.setListSubJobs(list, inp, test);
				for(Parts partRoute:r1.getPartsRoute()) {
					for(Edge e:partRoute.getDirectoryConnections().values()) {
						p.getDirectoryConnections().put(e.getEdgeKey(), e);
						r.getEdges().put(e.getEdgeKey(), e);
					}
				}
				for(Parts partRoute:r2.getPartsRoute()) {
					for(Edge e:partRoute.getDirectoryConnections().values()) {
						p.getDirectoryConnections().put(e.getEdgeKey(), e);
						r.getEdges().put(e.getEdgeKey(), e);
					}
				}
				r.getPartsRoute().add(p);
				r.updateRouteFromParts(inp, test, jobsInWalkingRoute);
			}	

		}
		if(!r.getPartsRoute().isEmpty()) {
			merge=true;
		}

		return merge;
	}

	private boolean insertingSubJobs(Route r1, Route r2, Route r) {
		boolean merging= false;
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		ArrayList <Parts> partList= new ArrayList <Parts>();
		for(int ii=2 ;ii<r2.getPartsRoute().size()-1;ii++) {
			partList.add(r2.getPartsRoute().get(ii));
		}
		int a=2;
		for(int i=1 ;i<r1.getPartsRoute().size()-1;i++) {
			SubJobs next=r1.getPartsRoute().get(i+1).getListSubJobs().get(0);
			boolean merge= false;
			Parts partEarly= r1.getPartsRoute().get(i);
			for(int ii=1 ;ii<r2.getPartsRoute().size()-1;ii++) {
				if(partList.contains(r2.getPartsRoute().get(ii))) {
					list= new ArrayList<SubJobs>();
					merge=mergingPartsChagingTimes(partEarly,r2.getPartsRoute().get(ii),next,list);
					if(merge) {
						partList.remove(r2.getPartsRoute().get(ii));
						merging=true;
						Parts part= new Parts();
						part.setListSubJobs(list, inp, test);
						r.getPartsRoute().add(part);
						partEarly= new Parts();
						for(Edge e:partEarly.getDirectoryConnections().values()) {
							r.getEdges().put(e.getEdgeKey(), e);
						}
						for(Edge e:r2.getPartsRoute().get(ii).getDirectoryConnections().values()) {
							r.getEdges().put(e.getEdgeKey(), e);
						}
					}
				}
			}
			if(!merge) {
				r.getPartsRoute().add(r1.getPartsRoute().get(i));
				for(Edge e:partEarly.getDirectoryConnections().values()) {
					r.getEdges().put(e.getEdgeKey(), e);
				}
			}
		}

		return merging;
	}



	private HashMap<String, SubJobs> listJobs(Route r1, Route r2) {
		HashMap<String, SubJobs> listJobs= new HashMap<String, SubJobs>();
		for(SubJobs j: r1.getSubJobsList()) {
			listJobs.put(j.getSubJobKey(), j);
		}

		for(SubJobs j: r2.getSubJobsList()) {
			listJobs.put(j.getSubJobKey(), j);
		}
		return listJobs;
	}

	private ArrayList<SubJobs> selectiondropOffJobsPatientsMC(Route r1, Route r2) {
		ArrayList<SubJobs> dropOffJobsPatientsMC= new ArrayList<SubJobs>();
		for(SubJobs j: r1.getSubJobsList()) {
			if(j.isMedicalCentre() && j.getTotalPeople()<0) {
				dropOffJobsPatientsMC.add(new SubJobs(j));}
		}
		for(SubJobs j: r2.getSubJobsList()) {
			if(j.isMedicalCentre() && j.getTotalPeople()<0) {
				dropOffJobsPatientsMC.add(new SubJobs(j));		
			}
		}
		return dropOffJobsPatientsMC;
	}


	private ArrayList<SubJobs> selectiondropOffJobsHHC(Route r1, Route r2) {
		ArrayList<SubJobs> dropOffJobsclient= new ArrayList<SubJobs>();
		for(SubJobs j: r1.getSubJobsList()) {
			if(j.isClient() && j.getTotalPeople()<0) {
				dropOffJobsclient.add(new SubJobs(j));	}
		}
		for(SubJobs j: r2.getSubJobsList()) {
			if(j.isClient() && j.getTotalPeople()<0) {
				dropOffJobsclient.add(new SubJobs(j));	
			}
		}
		return dropOffJobsclient;
	}



	void convertingWalkingRoutesInOneTask(ArrayList<Couple> coupleFromWalkingRoutes, WalkingRoutes subroutes2) {
		if(subroutes2.getWalkingRoutes()!=null) {
			for(SubRoute r:subroutes2.getWalkingRoutes()) {
				if(r.getDropOffNode()!=null && r.getPickUpNode()!=null) {
					double walkingRouteLength=r.getDurationWalkingRoute();

					// 0. creation of subjobs and fixing time windows 
					if(r.getDropOffNode().getId()==3) {
						//System.out.println("couple ");
					}
					Jobs present=creatinngPresentJobFromWR(r.getDropOffNode(),walkingRouteLength);

					Jobs future=creatinngFutureJobFromWR(present, r.getPickUpNode());
					//1. creation of couple
					Couple pairPickUpDropOffHCS=creatingCoupleClientHome(present,future); 

					// 2. fixing number of people involved
					settingPeopleInSubJob(pairPickUpDropOffHCS, -1,+1); //- drop-off #personas + pick up #personas

					// 4. adding couple
					coupleFromWalkingRoutes.add(pairPickUpDropOffHCS);
					// 3. checking information
					//System.out.println("couple ");
					//System.out.println(pairPickUpDropOffHCS.toString());
					//System.out.println("couple ");
					if(present.getstartServiceTime()>future.getstartServiceTime()) { // control
						//System.out.println("error");
					}

				}
			}
		}
	}

	private void settingPeopleInSubJob(Couple pairPatientMedicalCentre, int i, int j) {
		pairPatientMedicalCentre.getPresent().setTotalPeople(i); // 1 persona porque se recoge sólo al paciente
		pairPatientMedicalCentre.getFuture().setTotalPeople(j); // setting el numero de personas en el servicio // es un dos porque es el paramedico y el pacnete al mismo tiempo	
	}


	public Couple creatingCoupleClientHome(Jobs presentJob, Jobs futureJob) {
		presentJob.setPair(futureJob);
		int directConnectionDistance= inp.getCarCost().getCost(presentJob.getId()-1, futureJob.getId()-1); // setting the time for picking up the patient at home patient
		Couple pairPatientMedicalCentre=creatingPairPickUpDeliveryHCS(presentJob,futureJob, directConnectionDistance);
		return pairPatientMedicalCentre;
	}

	private Couple creatingPairPickUpDeliveryHCS(Jobs presentJob, Jobs futureJob, int directConnectionDistance) {
		Couple presentCouple= new Couple(presentJob,futureJob, directConnectionDistance, test.getDetour());
		return presentCouple;
	}

	private Jobs creatinngFutureJobFromWR(Jobs present,Jobs pickUpNode) {
		// homeCarePickUp(dropOff,pickUp);

		Jobs future= new Jobs(pickUpNode.getId(),0,0 ,pickUpNode.getReqQualification(), 0); // Jobs(int id, int startTime, int endTime, int reqQualification,int reqTime)


		future.setTotalPeople(1);
		future.setClient(true);	
		future.setserviceTime(0); //los nodos pick up contienen la información de los nodos
		// Setting the TW
		//double tv=inp.getCarCost().getCost(present.getId()-1, pickUpNode.getId()-1)*test.getDetour();
		double tv=inp.getCarCost().getCost(present.getId()-1, pickUpNode.getId()-1);
		future.setStartTime(present.getendServiceTime()+tv);
		future.setEndTime(future.getStartTime()+test.getCumulativeWaitingTime()); // considering waiting time

		// modificar el tiempo requerido para el trabajo+	
		// 1. Setting the start service time -- startServiceTime
		future.setStartServiceTime(future.getEndTime());
		// 3. Set el fin del servicio
		future.setEndServiceTime(future.getstartServiceTime());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		future.setarrivalTime(future.getstartServiceTime());
		future.setdepartureTime(future.getendServiceTime()+future.getloadUnloadTime());
		/// poner el tiempo de servicio en cero
		future.setserviceTime(0);
		// present
		double deltaArrivalDeparture=future.getDepartureTime()-future.getArrivalTime();
		double deltaArrivalStartServiceTime=future.getstartServiceTime()-future.getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=future.getendServiceTime()-future.getstartServiceTime();
		future.setdeltaArrivalDeparture(deltaArrivalDeparture);
		future.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		future.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);
		return future;
	}

	private Jobs creatinngPresentJobFromWR(Jobs dropOffNode, double walkingRouteLength) {
		// start when the service
		Jobs present= new Jobs(dropOffNode.getId(),dropOffNode.getStartTime(),dropOffNode.getEndTime() ,dropOffNode.getReqQualification(), walkingRouteLength); // Jobs(int id, int startTime, int endTime, int reqQualification,int reqTime)

		///
		present.setTotalPeople(-1);
		present.setClient(true);
		present.setloadUnloadTime(test.getloadTimeHomeCareStaff());
		// 1. Setting the start service time -- startServiceTime
		present.setStartServiceTime(present.getEndTime());
		// 2. Set ArrivalTime
		present.setarrivalTime(present.getstartServiceTime()-present.getloadUnloadTime());
		// 3. Set el fin del servicio
		present.setEndServiceTime(present.getstartServiceTime()+walkingRouteLength);	
		present.setdepartureTime(present.getendServiceTime()+present.getloadUnloadTime());


		///
		// present
		double deltaArrivalDeparture=present.getDepartureTime()-present.getArrivalTime();
		double deltaArrivalStartServiceTime=present.getstartServiceTime()-present.getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=present.getendServiceTime()-present.getstartServiceTime();
		present.setdeltaArrivalDeparture(deltaArrivalDeparture);
		present.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		present.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);
		return present;
	}




	private Solution intraMergingParts0(Solution copySolution) {
		Solution newSol= new Solution(copySolution); // 1. copy solution
		//System.out.println(newSol.toString());
		if(newSol.getRoutes().size()>=2) {
			// 2. Seleccionar las rutas que se van a mezclar
			for(int i=0;i<copySolution.getRoutes().size();i++) {
				int r2 = i;
				while(i==r2) {
					r2 = this.rn.nextInt(copySolution.getRoutes().size()-1);}

				Route refRoute=copySolution.getRoutes().get(i);
				Route toSplit=copySolution.getRoutes().get(r2);
				if(toSplit.getJobsDirectory().containsKey("D6") || refRoute.getJobsDirectory().containsKey("D6")) {
					//System.out.println("***Parte vacia");
				}
				boolean mergingParts= insertingPartIntoPartSorting(refRoute,toSplit); // true <- cuando las rutas estan cambiando (se mezclan) false <-cuando las rutas permanecen igual

				//System.out.println("***Parte vacia");

			}


			//System.out.println("***Parte vacia");
			//System.out.println(newSol.toString());
		}
		ArrayList<Route> newRoute= new ArrayList<Route>();
		for(Route r:newSol.getRoutes()) {
			if(!r.getPartsRoute().isEmpty()) {
				newRoute.add(r);}
		}
		newSol.getRoutes().clear();
		for(Route r:newRoute) {
			if(!r.getPartsRoute().isEmpty()) {
				newSol.getRoutes().add(r);
			}
		}
		newSol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
		return newSol;
	}


	private boolean insertingPartIntoPartSorting(Route refRoute, Route toSplit) {
		boolean merging=false;
		boolean mergingParts=false;
		if((refRoute.getDurationRoute()+toSplit.getDurationRoute())<=test.getRouteLenght()) {
			//if((refRoute.getTravelTime()+refRoute.getloadUnloadRegistrationTime()+toSplit.getTravelTime()+toSplit.getloadUnloadRegistrationTime())<test.getRouteLenght()) {
			mergingParts=true;
		}
		if(refRoute!=toSplit && mergingParts && !refRoute.getPartsRoute().isEmpty() && !toSplit.getPartsRoute().isEmpty()) { // sólo se pueden mezclar si son rutas diferentes
			// se tiene que tener cuidado con las working horas y con los detours

			if(refRoute.getIdRoute()==0 && toSplit.getIdRoute()==1) {
				//System.out.println("Stop");
			}
			boolean node=false;
			Route newRoute= new Route(); // la ruta que va a almacenar 
			Route earlyRoute=selecctingStartRoute(refRoute,toSplit);
			Route lateRoute=selecctingRouteToInsert(refRoute,toSplit);
			boolean mergingTrips= mergingIndividualTrips(earlyRoute,lateRoute,newRoute);
			boolean mergingHeading=false;
			boolean mergingIntermediateParts=false;
			if(!mergingTrips) {
				if(earlyRoute.getPartsRoute().size()>4 && refRoute.getPartsRoute().size()>4) {

					// 1. Evaluar mezclar las primeras partes 


					mergingHeading=seattingFirstPart(earlyRoute,lateRoute,newRoute);
					// 2. Evaluar mezclar las partes intermedias
					if(mergingHeading ) {
						//System.out.println("Stop");
					}

					mergingIntermediateParts=settingMiddleParts(earlyRoute,lateRoute,newRoute);
					if(mergingIntermediateParts) {
						//System.out.println("Stop");
					}

					if(mergingIntermediateParts || mergingHeading ) {
						mergingTrips=true;
					}
				}
			}

			if(mergingTrips ) {
				updatingEarlyRoute(earlyRoute,newRoute,lateRoute);
			}

			// los trabajos que aparecen en la ruta nueva no tienen porque aparecer en la ruta vieja
			//System.out.println("Stop");
			//System.out.println("Route earlyRoute"  +earlyRoute.toString());
			//System.out.println("Route Late"  +lateRoute.toString());
			//System.out.println("Stop");


		}
		return merging;
	}

	private void updatingEarlyRoute(Route earlyRoute, Route newRoute, Route lateRoute) {
		// early Route
		newRoute.getPartsRoute().add(0,earlyRoute.getPartsRoute().get(0));
		newRoute.getPartsRoute().add(earlyRoute.getPartsRoute().get(earlyRoute.getPartsRoute().size()-1));
		earlyRoute.getPartsRoute().clear();
		earlyRoute.getSubJobsList().clear();
		earlyRoute.getEdges().clear();
		for(Parts p:newRoute.getPartsRoute()) {
			earlyRoute.getPartsRoute().add(p);
		}
		earlyRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);

		for(Edge e:newRoute.getEdges().values()) {
			earlyRoute.getEdges().put(e.getEdgeKey(), e);
		}

		// late route
		ArrayList<Parts> list= new ArrayList<Parts>();
		for(int index=1;index<lateRoute.getPartsRoute().size()-1;index++) {
			Parts p=lateRoute.getPartsRoute().get(index);
			boolean isInOtherRoute=checkifJobInEarlyRoute(p,earlyRoute);
			if(!isInOtherRoute) {
				list.add(p);
			}
		}
		if(!list.isEmpty()) {
			list.add(0,lateRoute.getPartsRoute().get(0));
			list.add(lateRoute.getPartsRoute().get(lateRoute.getPartsRoute().size()-1));
		}
		lateRoute.getPartsRoute().clear();
		lateRoute.getSubJobsList().clear();
		if(!list.isEmpty()) {
			for(Parts p:list) {
				lateRoute.getPartsRoute().add(p);
			}
			lateRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);}
	}

	private boolean checkifJobInEarlyRoute(Parts p, Route earlyRoute) {
		boolean inRoute=false;
		for(SubJobs j:p.getListSubJobs()) {
			if(earlyRoute.getJobsDirectory().containsKey(j.getSubJobKey())) {
				inRoute=true;
				break;
			}
		}
		return inRoute;
	}

	private boolean mergingIndividualTrips(Route early, Route late, Route newRoute) {
		boolean merging= false;
		if(early.getSubJobsList().get(early.getSubJobsList().size()-1).getDepartureTime()<late.getSubJobsList().get(0).getArrivalTime()) {
			merging= true;
			for(int i = 1; i<early.getPartsRoute().size()-1;i++) {
				newRoute.getPartsRoute().add(early.getPartsRoute().get(i));
			}
			for(int i = 1; i<late.getPartsRoute().size()-1;i++) {
				newRoute.getPartsRoute().add(late.getPartsRoute().get(i));
			}
			for(Edge e:early.getEdges().values()) {
				newRoute.getEdges().put(e.getEdgeKey(), e);
			}
			for(Edge e:late.getEdges().values()) {
				newRoute.getEdges().put(e.getEdgeKey(), e);
			}
		}

		return merging;
	}

	private boolean settingMiddleParts(Route earlyRoute, Route lateRoute, Route newRoute) {
		boolean merging= false;
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		ArrayList <Parts> partList= new ArrayList <Parts>();
		for(int ii=2 ;ii<lateRoute.getPartsRoute().size()-1;ii++) {
			partList.add(lateRoute.getPartsRoute().get(ii));
		}
		int a=2;
		for(int i=2 ;i<earlyRoute.getPartsRoute().size()-1;i++) {
			SubJobs next=earlyRoute.getPartsRoute().get(i+1).getListSubJobs().get(0);
			boolean merge= false;
			Parts partEarly= earlyRoute.getPartsRoute().get(i);
			for(int ii=2 ;ii<lateRoute.getPartsRoute().size()-1;ii++) {
				if(partList.contains(lateRoute.getPartsRoute().get(ii))) {
					list= new ArrayList<SubJobs>();
					merge=mergingParts(partEarly,lateRoute.getPartsRoute().get(ii),next,list);
					if(merge) {
						partList.remove(lateRoute.getPartsRoute().get(ii));
						merging=true;
						Parts part= new Parts();
						part.setListSubJobs(list, inp, test);
						newRoute.getPartsRoute().add(part);
						partEarly= new Parts();
						for(Edge e:partEarly.getDirectoryConnections().values()) {
							newRoute.getEdges().put(e.getEdgeKey(), e);
						}
						for(Edge e:lateRoute.getPartsRoute().get(ii).getDirectoryConnections().values()) {
							newRoute.getEdges().put(e.getEdgeKey(), e);
						}
					}
				}
			}
			if(!merge) {
				newRoute.getPartsRoute().add(earlyRoute.getPartsRoute().get(i));
				for(Edge e:partEarly.getDirectoryConnections().values()) {
					newRoute.getEdges().put(e.getEdgeKey(), e);
				}
			}
		}

		return merging;
	}
	//	private boolean settingMiddleParts(Route earlyRoute, Route lateRoute, Route newRoute) {
	//		boolean merging= false;
	//		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
	//		for(int i=2 ;i<earlyRoute.getPartsRoute().size()-1;i++) {
	//			for(SubJobs j:earlyRoute.getPartsRoute().get(i).getListSubJobs()) {
	//				list.add(j);
	//			}
	//		}
	//		for(int i=2 ;i<lateRoute.getPartsRoute().size()-1;i++) {
	//			for(SubJobs j:lateRoute.getPartsRoute().get(i).getListSubJobs()) {
	//				list.add(j);
	//			}
	//		}
	//
	//		list.sort(Jobs.SORT_BY_ARRIVALTIME);
	//
	//		//ArrayList<SubJobs> newSequence= sortingSequence(list);
	//		boolean feasible= evaluatingArrivalTime(list);
	//		if(feasible) {
	//			if(vehicleCapacityPart(list)) { // checking the vehicle capacity
	//				merging=true;	
	//				Parts part= new Parts();
	//				part.setListSubJobs(list, inp, test);
	//				newRoute.getPartsRoute().add(part);
	//			}	
	//
	//		}
	//		if(!merging) {
	//			for(int i=2 ;i<earlyRoute.getPartsRoute().size()-1;i++) {
	//				newRoute.getPartsRoute().add(earlyRoute.getPartsRoute().get(i));
	//			}
	//		}
	//		return merging;
	//	}

	private boolean mergingPartsChagingTimes(Parts parts, Parts parts2, SubJobs next,ArrayList<SubJobs> list) {
		boolean merge=false;
		for(SubJobs j:parts.getListSubJobs()) {
			list.add(j);
		}
		for(SubJobs j:parts2.getListSubJobs()) {
			list.add(j);
		}
		list.sort(Jobs.SORT_BY_ARRIVALTIME);

		//ArrayList<SubJobs> newSequence= sortingSequence(list);
		boolean feasible= evaluatingArrivalTimeChangingTime(list);
		SubJobs lastJobSequence=list.get(list.size()-1);
		double tv=inp.getCarCost().getCost(lastJobSequence.getId()-1, next.getId()-1);
		double timeNexpart=Double.MAX_VALUE;
		if(next.getId()!=1) {
			timeNexpart=next.getArrivalTime();
		}
		if(feasible && lastJobSequence.getDepartureTime()+tv<timeNexpart) {
			if(vehicleCapacityPart(list)) { // checking the vehicle capacity
				merge=true;	
			}	

		}

		return merge;
	}

	private boolean mergingParts(Parts parts, Parts parts2, SubJobs next,ArrayList<SubJobs> list) {
		boolean merge=false;
		for(SubJobs j:parts.getListSubJobs()) {
			list.add(j);
		}
		for(SubJobs j:parts2.getListSubJobs()) {
			list.add(j);
		}
		list.sort(Jobs.SORT_BY_ARRIVALTIME);

		//ArrayList<SubJobs> newSequence= sortingSequence(list);
		boolean feasible= evaluatingArrivalTime(list);
		SubJobs lastJobSequence=list.get(list.size()-1);
		double tv=inp.getCarCost().getCost(lastJobSequence.getId()-1, next.getId()-1);
		double timeNexpart=Double.MAX_VALUE;
		if(next.getId()!=1) {
			timeNexpart=next.getArrivalTime();
		}
		if(feasible && lastJobSequence.getDepartureTime()+tv<timeNexpart) {
			if(vehicleCapacityPart(list)) { // checking the vehicle capacity
				merge=true;	
			}	

		}

		return merge;
	}

	private boolean seattingFirstPart(Route earlyRoute, Route lateRoute, Route newRoute) {
		boolean merging= false;
		Parts firstEarlyPart=earlyRoute.getPartsRoute().get(1);
		Parts nextEarlyPart=earlyRoute.getPartsRoute().get(2);
		Parts firstLatePart=lateRoute.getPartsRoute().get(1);
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		for(SubJobs j:firstEarlyPart.getListSubJobs()) {
			list.add(j);
		}
		for(SubJobs j:firstLatePart.getListSubJobs()) {
			list.add(j);
		}
		list.sort(Jobs.SORT_BY_ARRIVALTIME);

		//ArrayList<SubJobs> newSequence= sortingSequence(list);
		boolean feasible= evaluatingArrivalTime(list);
		if(feasible) {
			SubJobs lastJobs=list.get(list.size()-1);
			double travelTime=inp.getCarCost().getCost(lastJobs.getId()-1, nextEarlyPart.getListSubJobs().get(0).getId()-1);
			if(lastJobs.getDepartureTime()+travelTime<nextEarlyPart.getListSubJobs().get(0).getArrivalTime()) { // checking tw for the next part
				if(vehicleCapacityPart(list)) { // checking the vehicle capacity
					merging=true;	
					Parts part= new Parts();
					part.setListSubJobs(list, inp, test);
					newRoute.getPartsRoute().add(part);
					for(Edge e:firstEarlyPart.getDirectoryConnections().values()) {
						newRoute.getEdges().put(e.getEdgeKey(), e);
					}
					for(Edge e:firstLatePart.getDirectoryConnections().values()) {
						newRoute.getEdges().put(e.getEdgeKey(), e);
					}
				}	
			}
		}
		if(!merging) {	
			newRoute.getPartsRoute().add(firstEarlyPart);
			for(Edge e:firstEarlyPart.getDirectoryConnections().values()) {
				newRoute.getEdges().put(e.getEdgeKey(), e);
			}
		}
		return merging;
	}

	private ArrayList<SubJobs> sortingSequence(ArrayList<SubJobs> list) {
		ArrayList<SubJobs> newSequence= new ArrayList<SubJobs>();
		for(SubJobs j: list) {
			if(j.isMedicalCentre() && j.getTotalPeople()<0) {
				newSequence.add(j);
			}
		}
		for(SubJobs j: list) {
			if(!newSequence.contains(j)) {
				if(j.isClient() && j.getTotalPeople()<0) {
					boolean early=isFirstJob(newSequence,j);
					if(!early) {
						early=isAnIntermediateStopDropOff(newSequence,j);
						if(!early) { // last jobs in the sequence

						}
					}
				}
			}
		}
		return newSequence;
	}

	private boolean isAnIntermediateStopDropOff(ArrayList<SubJobs> newSequence, SubJobs j) {
		boolean inserted= false;
		for(int i=1; i<newSequence.size();i++) {
			SubJobs previous=newSequence.get(i-1);
			SubJobs next=newSequence.get(i);
			double previousTVj=inp.getCarCost().getCost(previous.getId()-1, j.getId()-1);
			double jTVnext=inp.getCarCost().getCost(j.getId()-1, next.getId()-1);
			double possibleArrival=previous.getDepartureTime()+previousTVj;
			double possibleStartServiceTime=possibleArrival+j.getloadUnloadTime();// start service time = departure time
			if(possibleArrival<=j.getArrivalTime()) {
				if(possibleStartServiceTime+jTVnext<=next.getArrivalTime()) {
					j.setarrivalTime(possibleArrival);
					j.setStartServiceTime(Math.max(possibleStartServiceTime, j.getStartTime()));
					j.setEndServiceTime(j.getReqTime());
					j.setdepartureTime(possibleStartServiceTime);
				}
			}
		}
		return inserted;
	}

	private boolean isFirstJob(ArrayList<SubJobs> newSequence, SubJobs j) {
		boolean inserted= false;
		if(!newSequence.isEmpty()) {
			if(j.getArrivalTime()<newSequence.get(0).getArrivalTime()) {
				newSequence.add(0,j);
				inserted=true;
			}
		}
		else {
			newSequence.add(j);
			inserted=true;
		}
		return inserted;
	}

	private boolean evaluatingArrivalTime(ArrayList<SubJobs> list) {
		boolean feasible= true;
		for(int index=1;index<list.size();index++) {
			SubJobs i= list.get(index-1);
			SubJobs j= list.get(index);
			double tv= inp.getCarCost().getCost(i.getId()-1, j.getId()-1);
			if(i.getDepartureTime()+tv>j.getArrivalTime()) {
				feasible=false;
			}
			if(!feasible) {
				break;
			}
		}
		return feasible;
	}



	private boolean evaluatingArrivalTimeChangingTime(ArrayList<SubJobs> list) {
		boolean feasible= true;
		double [] timeDeltas= new double [list.size()] ;
		if(list.get(0).isMedicalCentre() || list.get(0).isClient() ) {
			if(list.get(0).getTotalPeople()<0) {
				timeDeltas[0]=list.get(0).getstartServiceTime()-list.get(0).getStartTime();
			}
		}
		for(int index=1;index<list.size();index++) {

			SubJobs i= list.get(index-1);
			SubJobs j= list.get(index);
			timeDeltas[index]=j.getArrivalTime()-j.getStartTime();
			double tv= inp.getCarCost().getCost(i.getId()-1, j.getId()-1);
			double possibleArrival=j.getArrivalTime()-(i.getDepartureTime()+tv);
			if(j.isMedicalCentre() || j.isClient() ) {
				if(j.getTotalPeople()<0) {
					timeDeltas[index]=possibleArrival;
				}
			}
		}
		SubJobs criticalNode=null;
		double accumulative=-1;
		int position=-1;
		do{
			for(int i=0;i<list.size();i++) {
				if(timeDeltas[i]<0) {
					position=i;
					criticalNode=list.get(i);
					break;
				}
				else {
					accumulative+=timeDeltas[i];
				}
			}

			// selecting drop-off patients and home care staff
			SubJobs node=null;
			if(position>=0 && accumulative>Math.abs(timeDeltas[position]) ) {
				for(int i=0;i<position;i++) {
					node= list.get(i);
					System.out.print(timeDeltas[position]);
					list.get(i).setarrivalTime(list.get(i).getArrivalTime()+timeDeltas[position]);
					list.get(i).setdepartureTime(list.get(i).getDepartureTime()+timeDeltas[position]);
					double startServiceTime=list.get(i).getArrivalTime()+list.get(i).getdeltaArrivalStartServiceTime();
					list.get(i).setStartServiceTime(Math.max(startServiceTime, list.get(i).getStartTime()));
					double endServiceTime=list.get(i).getstartServiceTime()+list.get(i).getdeltarStartServiceTimeEndServiceTime();
					list.get(i).setEndServiceTime(endServiceTime);
				}
			}

			for(int index=1;index<list.size();index++) {
				SubJobs i= list.get(index-1);
				SubJobs j= list.get(index);
				timeDeltas[index]=j.getArrivalTime()-j.getStartTime();
				double tv= inp.getCarCost().getCost(i.getId()-1, j.getId()-1);
				double possibleArrival=j.getArrivalTime()-(i.getDepartureTime()+tv);
				if(j.isMedicalCentre() || j.isClient() ) {
					if(j.getTotalPeople()<0) {
						timeDeltas[index]=possibleArrival;
					}
				}
			}

			accumulative=-1;
			position=-1;
			for(int i=0;i<list.size();i++) {
				if(timeDeltas[i]<0) {
					position=i;
					criticalNode=list.get(i);
					break;
				}
				else {
					accumulative+=timeDeltas[i];
				}
			}

		}while(position>=0 && accumulative>Math.abs(timeDeltas[position])) ;
		//System.out.println("Stop");
		if(position==-1 && accumulative>0) {
			feasible=true;
		}
		else {
			feasible=false;
		}
		return feasible;
	}









	private void changinPartTime(Parts a) {
		for(SubJobs j:a.getListSubJobs()) {
			// if pick up time el tiempo cuenta
			double departure=0;
			if(j.isClient()) {
				departure=j.getArrivalTime()+test.getloadTimeHomeCareStaff();
			}
			else {
				departure=j.getArrivalTime()+test.getloadTimePatient();
			}
			j.setdepartureTime(departure);
		}

	}



	private void settingNewPart(Solution sol) {
		for(Route r:sol.getRoutes()) {
			int passengers=1;
			ArrayList<Parts> parts= new ArrayList<>();
			ArrayList<SubJobs> listSubJobs= new ArrayList<>();
			parts.add(r.getPartsRoute().get(0));
			for(SubJobs j:r.getSubJobsList()) {
				passengers+=j.getTotalPeople();
				listSubJobs.add(j);
				if(passengers==0) {
					Parts part=new Parts();
					if(!listSubJobs.isEmpty()) {
						part.setListSubJobs(listSubJobs, inp, test);
						parts.add(part);}
					listSubJobs= new ArrayList<>();
				}
				if(r.getSubJobsList().getLast()==j) {
					Parts part=new Parts();
					if(!listSubJobs.isEmpty()) {
						part.setListSubJobs(listSubJobs, inp, test);
						parts.add(part);}
				}
			}
			parts.add(r.getPartsRoute().getLast());
			r.getPartsRoute().clear();
			for(Parts p:parts) {
				r.getPartsRoute().add(p);
			}
		}
	}

	private Solution intraMergingParts(Solution copySolution) {

		Solution newSol= new Solution(copySolution); // 1. copy solution
		// 2. Seleccionar las rutas que se van a mezclar
		ArrayList<Route> copyRoute = copyListRoute(copySolution); // copy original route for safe
		for(Route refRoute:copySolution.getRoutes()) {
			for(Route toSplit:copySolution.getRoutes()) {
				if(toSplit.getPartsRoute().get(1).getListSubJobs().get(0).getId()==25 || refRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==25) {
					//System.out.println("***Parte vacia");
				}
				boolean mergingParts= insertingPartIntoPart(refRoute,toSplit); // true <- cuando las rutas estan cambiando (se mezclan) false <-cuando las rutas permanecen igual
				//System.out.println("***Parte vacia");
			}
		}


		//System.out.println("***Parte vacia");
		//System.out.println(newSol.toString());

		return newSol;
	}

	private boolean insertingPartIntoPart(Route refRoute, Route toSplit) {
		boolean merging=false;
		boolean mergingParts=false;
		if((refRoute.getDurationRoute()+toSplit.getDurationRoute())<=test.getRouteLenght()) {
			//if((refRoute.getTravelTime()+refRoute.getloadUnloadRegistrationTime()+toSplit.getTravelTime()+toSplit.getloadUnloadRegistrationTime())<test.getRouteLenght()) {
			mergingParts=true;
		}
		if(refRoute!=toSplit && mergingParts) { // sólo se pueden mezclar si son rutas diferentes
			// se tiene que tener cuidado con las working horas y con los detours

			if(refRoute.getIdRoute()==7 && toSplit.getIdRoute()==4) {
				//System.out.println("Stop");
			}
			boolean node=false;
			Route earlyRoute=selecctingStartRoute(refRoute,toSplit);
			Route lateRoute=selecctingRouteToInsert(refRoute,toSplit);
			//if(earlyRoute==refRoute) {
			Route newRoute= new Route(); // la ruta que va a almacenar 

			// 1. Evaluar mezclar las primeras partes 
			boolean mergingHeading=seatingHeading(earlyRoute,lateRoute,newRoute);
			// 2. Evaluar mezclar las partes intermedias
			if(mergingHeading ) {
				//System.out.println("Stop");
			}

			boolean mergingIntermediateParts=settingIntermediateParts(earlyRoute,lateRoute,newRoute);
			if(mergingIntermediateParts) {
				//System.out.println("Stop");
			}

			// 3. Evaluar mezclar las ultimas partes
			//TO DO: boolean mergingTail=settingTailsParts(earlyRoute,lateRoute,mergingPart);
			// 4. Los trabajos contenidos en la nueva ruta corresponden a los nuevos trabajos que va a integrar la ruta más temprana
			boolean mergingLastParts=settingTailsParts(earlyRoute,lateRoute,newRoute);
			if(mergingLastParts) {
				//System.out.println("Stop");
			}
			updatingRefRoute(earlyRoute,newRoute);
			updatingLateRoute(lateRoute,earlyRoute,newRoute); // los trabajos que aparecen en la ruta nueva no tienen porque aparecer en la ruta vieja
			//System.out.println("Stop");
			//System.out.println("Route earlyRoute"  +earlyRoute.toString());
			//System.out.println("Route Late"  +lateRoute.toString());
			//System.out.println("Stop");
			//}
		}
		return merging;
	}

	private boolean settingTailsParts(Route earlyRoute, Route lateRoute, Route newRoute) {
		boolean merge =false;
		ArrayList<Parts> partsList=selectingParts(earlyRoute,lateRoute);// 0<- parte más larga 1<- parte más corta
		//System.out.println(earlyRoute.toString());
		//System.out.println(lateRoute.toString());
		if(partsList.get(0).getListSubJobs().size()>1 && partsList.get(1).getListSubJobs().size()>1 ) {
			merge=settingTailsRoute(partsList,newRoute);	
		}
		else {
			merge=settingTails(partsList,newRoute);
		}
		if(!merge) {
			if(earlyRoute.getPartsRoute().size()>3) {
				Parts p=earlyRoute.getPartsRoute().get(earlyRoute.getPartsRoute().size()-2);
				Parts newPart=new Parts(p);
				newRoute.getPartsRoute().add(newPart);
				newPart.settingConnections( partsList.get(0),  partsList.get(1));
				newRoute.getPartsRoute().add(newPart);
			}
			Parts newPart=new Parts(earlyRoute.getPartsRoute().getLast());
			newRoute.getPartsRoute().add(newPart);
			//newRoute.getSubJobsList().add(earlyRoute.getPartsRoute().getLast().getListSubJobs().get(0));
		}
		else {
			Parts newPart=new Parts(earlyRoute.getPartsRoute().getLast());
			newRoute.getPartsRoute().add(newPart);
			newPart.settingConnections( partsList.get(0),  partsList.get(1));
			newRoute.getPartsRoute().add(newPart);
			//newRoute.getSubJobsList().add(earlyRoute.getPartsRoute().getLast().getListSubJobs().get(0));
		}

		return merge;
	}

	//	private boolean settingTailsParts(Route earlyRoute, Route lateRoute, Route newRoute) {
	//		boolean merge =false;
	//		ArrayList<Parts> partsList=selectingParts(earlyRoute,lateRoute);// 0<- parte más larga 1<- parte más corta
	//
	//		if(partsList.get(0).getListSubJobs().size()>1 && partsList.get(1).getListSubJobs().size()>1 ) {
	//			mergingPart(partsList.get(0),partsList.get(1),newRoute,merge);
	//			if(merge) {
	//				Parts newPart=new Parts(earlyRoute.getPartsRoute().getLast());
	//				newRoute.getPartsRoute().add(newPart);
	//				newPart.settingConnections( partsList.get(0),  partsList.get(1));}
	//			else {
	//				Parts p=earlyRoute.getPartsRoute().get(earlyRoute.getPartsRoute().size()-2);
	//				Parts newPart=new Parts(p);
	//				newRoute.getPartsRoute().add(newPart);
	//				newPart.settingConnections( partsList.get(0),  partsList.get(1));
	//				newRoute.getPartsRoute().add(newPart);
	//			}
	//		}
	//		return merge;
	//	}



	private boolean settingTails(ArrayList<Parts> partsList, Route newRoute) {
		boolean merge=false;
		ArrayList<SubJobs> list=new ArrayList<SubJobs>();


		SubJobs toInsert=partsList.get(1).getListSubJobs().get(0);
		SubJobs depot=new SubJobs(inp.getNodes().get(0));
		Edge edgeShortPart=new Edge(toInsert,depot,inp,test);
		merge=earlyInsertion(toInsert,edgeShortPart,partsList);
		if(merge) {
			newRoute.getSubJobsList().add(toInsert);
			for(int i=0;i<partsList.get(0).getListSubJobs().size();i++) {
				newRoute.getSubJobsList().add(partsList.get(0).getListSubJobs().get(i));
			}
		}
		else {
			for(int i=0;i<partsList.get(0).getListSubJobs().size()-1;i++) {
				//detour=checkingDetour(edgeLongPart,edgeShortPart,partsList);
				String key=partsList.get(0).getListSubJobs().get(i).getSubJobKey()+partsList.get(0).getListSubJobs().get(i+1).getSubJobKey();
				Edge edgeLongPart=partsList.get(0).getDirectoryConnections().get(key);	
				double remainingDistanceshortPart=remainingDistance(-1,partsList);// el toInsert es más tarde que el primer trabajo en la parte larga
				if(remainingDistanceshortPart<=edgeShortPart.getDetour()) {
					double origentoInsert=inp.getCarCost().getCost(edgeLongPart.getOrigin().getId()-1, toInsert.getId()-1);
					double toInsertEnd=inp.getCarCost().getCost(toInsert.getId()-1,edgeLongPart.getEnd().getId()-1);
					double distance=origentoInsert+toInsertEnd;
					if(distance<=edgeLongPart.getDetour()) { // detour insertando el nodo toInsert en la connexion edgeLongPart
						if(edgeLongPart.getOrigin().getDepartureTime()+origentoInsert<toInsert.getArrivalTime()) {// ventana de tiempo
							if(toInsert.getDepartureTime()+toInsertEnd<edgeLongPart.getEnd().getArrivalTime()) {// ventana de tiempo

								insertingTailRoute(list,partsList,i,0);
								if(vehicleCapacityPart(list)) {
									merge=true;
								}
								else {
									list.clear();
								}
							}
						}
					}
				}
				if(!merge) {
					if(!list.isEmpty()) {
						for(SubJobs k:list) {
							newRoute.getSubJobsList().add(k);
						}
					}
					break;
				}
			}
		}
		return merge;
	}

	private boolean vehicleCapacityPart(ArrayList<SubJobs> list) {
		boolean enoughCapacity=false;
		boolean enoughDepot=false;
		boolean feasible=false;
		int passegers=0;
		int action=0;
		boolean patient=false;
		int a=0;
		for(SubJobs s: list) {
			if(!patient && s.isPatient() && s.getTotalPeople()>0) {
				patient=true;
				a=1;
			}
			action=s.getTotalPeople()+a;
			passegers+=action;
			if(Math.abs(passegers)>inp.getVehicles().get(0).getMaxCapacity()) {
				enoughCapacity=false;
				break;
			}
		}
		if(Math.abs(passegers)>inp.getVehicles().get(0).getMaxCapacity()) {
			enoughCapacity=false;
		}
		else {
			enoughCapacity=true;
		}

		enoughDepot=goingoutFromDepot(list);

		if(enoughCapacity && enoughDepot) {
			feasible=true;
		}
		return feasible;
	}

	private boolean goingoutFromDepot(ArrayList<SubJobs> list) {
		boolean enoughCapacity=false;

		double homeCareStaff=0;
		double paramedic=0;

		double auxhhc=0;
		double auxparamedic=0;
		for(SubJobs j:list) {
			if(j.getTotalPeople()>0 && j.isPatient()) {
				auxparamedic+=j.getTotalPeople();
				if(auxparamedic!=0) {
					paramedic++;
				}
			}
			if(j.getTotalPeople()<0 && j.isPatient()) {
				auxparamedic+=j.getTotalPeople();
			}

			if(j.getTotalPeople()<0 && j.isClient()) {
				auxhhc+=j.getTotalPeople();
				if(auxhhc!=0) {
					homeCareStaff++;
				}
			}
			if(j.getTotalPeople()>0 && j.isClient()) {
				auxhhc+=j.getTotalPeople();
			}

		}
		if(auxhhc>homeCareStaff) {
			homeCareStaff=auxhhc;
		}
		if(auxparamedic>paramedic) {
			paramedic=auxparamedic;
		}
		if((paramedic+homeCareStaff)<=inp.getVehicles().get(0).getMaxCapacity()) {
			enoughCapacity=true;
		}
		//System.out.println("total HHC"+ homeCareStaff);
		//System.out.println("total Paramedic"+ paramedic);
		//System.out.println("total");


		return enoughCapacity;
	}



	private boolean earlyInsertion(SubJobs toInsert, Edge edgeShortPart, ArrayList<Parts> partsList) {
		boolean merge=false;
		if(toInsert.getDepartureTime()<partsList.get(0).getListSubJobs().get(0).getDepartureTime() && toInsert.getDepartureTime()<partsList.get(0).getListSubJobs().get(0).getArrivalTime()) {
			double remainingDistanceshortPart=remainingDistance(-1,partsList);// el toInsert es más tarde que el primer trabajo en la parte larga
			if(remainingDistanceshortPart<=edgeShortPart.getDetour()) {
				merge=true;
			}	
		}
		return merge;
	}

	private boolean settingTailsRoute(ArrayList<Parts> partsList, Route newRoute) {
		boolean merging=false;
		ArrayList<SubJobs> list=new ArrayList<SubJobs>();
		Parts newPart=new Parts();
		for(int i=0;i<partsList.get(0).getListSubJobs().size()-1;i++) {// large {0,1,2,3}
			for(int j=0;j<partsList.get(1).getListSubJobs().size()-1;j++) { // short {a,b}
				//{a,0,b,1,2,3} or {0,a,1,b,2,3}
				merging=false;
				boolean detour=false;
				String key=partsList.get(0).getListSubJobs().get(i).getSubJobKey()+partsList.get(0).getListSubJobs().get(i+1).getSubJobKey();
				Edge edgeLongPart=partsList.get(0).getDirectoryConnections().get(key);


				key=partsList.get(1).getListSubJobs().get(j).getSubJobKey()+partsList.get(1).getListSubJobs().get(j+1).getSubJobKey();
				Edge edgeShortPart=partsList.get(1).getDirectoryConnections().get(key);

				detour=checkingDetour(edgeLongPart,edgeShortPart,partsList);
				if(detour) {
					double tvShortLarge=inp.getCarCost().getCost(edgeShortPart.getOrigin().getId()-1, edgeLongPart.getOrigin().getId()-1);
					if(edgeShortPart.getOrigin().getDepartureTime()+tvShortLarge<=edgeLongPart.getOrigin().getArrivalTime()) {// checking time window (short O)---(large O)---(short E)---(large E)
						double tvLargeShort=inp.getCarCost().getCost(edgeLongPart.getOrigin().getId()-1, edgeShortPart.getEnd().getId()-1);
						if(edgeLongPart.getOrigin().getDepartureTime()+tvLargeShort<=edgeShortPart.getEnd().getArrivalTime()) {// checking time window (large O)---(short E)
							double tvShortLargeE=inp.getCarCost().getCost(edgeShortPart.getEnd().getId()-1, edgeLongPart.getEnd().getId()-1);
							if(edgeShortPart.getEnd().getDepartureTime()+tvShortLargeE<=edgeLongPart.getEnd().getArrivalTime()) {// checking time window (short E)---(large E)

								insertingTailRoute(list,partsList,i,j);
								if(vehicleCapacityPart(list)) {
									merging=true;
								}
								else {
									list.clear();
								}
							}	
						}	
					}
					if(!merging && (i+1)==(partsList.get(0).getListSubJobs().size()-1) ) {
						// (short O)---(Last subJob Large O)--- (short End) ----(resto de la parte short)---(Depot)
						if(edgeShortPart.getOrigin().getDepartureTime()+tvShortLarge<=edgeLongPart.getOrigin().getArrivalTime()) {// checking TW
							double tvLargeShort=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, edgeShortPart.getEnd().getId()-1);
							if(edgeLongPart.getOrigin().getDepartureTime()+tvLargeShort<=edgeShortPart.getEnd().getArrivalTime()) {
								double remainingDistanceshortPart=remainingDistance(j,partsList);
								double currentDistDepot=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, inp.getNodes().get(0).getId()-1)*(1+test.getDetour());
								if(remainingDistanceshortPart<=currentDistDepot) {// detour long
									insertingTailRoute(list,partsList,i,j);
									if(vehicleCapacityPart(list)) {
										merging=true;
									}
									else {
										list.clear();
									}
								}
							}
						}
					}	
				}
				if(!merging) {
					break;
				}
				else {
					if(!list.isEmpty()) {
						newPart.setListSubJobs(list, inp, test);
						//						for(SubJobs k:list) {
						//							newRoute.getSubJobsList().add(k);
						//						}
					}
				}
			}
		}
		if(!merging) {
			String key=partsList.get(0).getListSubJobs().get(partsList.get(0).getListSubJobs().size()-2).getSubJobKey()+partsList.get(0).getListSubJobs().get(partsList.get(0).getListSubJobs().size()-1).getSubJobKey();
			Edge edgeLongPart=partsList.get(0).getDirectoryConnections().get(key);
			key=partsList.get(1).getListSubJobs().get(partsList.get(1).getListSubJobs().size()-2).getSubJobKey()+partsList.get(1).getListSubJobs().get(partsList.get(1).getListSubJobs().size()-1).getSubJobKey();
			Edge edgeShortPart=partsList.get(1).getDirectoryConnections().get(key);
			if(edgeShortPart==null) {
				System.out.print("stop");
			}
			boolean detour=checkingDetour(edgeLongPart,edgeShortPart,partsList);
			if(detour) {
				double tvShortLarge=inp.getCarCost().getCost(edgeShortPart.getOrigin().getId()-1,edgeLongPart.getOrigin().getId()-1);
				if(edgeShortPart.getOrigin().getDepartureTime()+tvShortLarge<=edgeLongPart.getOrigin().getArrivalTime()) {// checking TW
					double tvLargeShort=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, edgeShortPart.getEnd().getId()-1);
					if(edgeLongPart.getOrigin().getDepartureTime()+tvLargeShort<=edgeShortPart.getEnd().getArrivalTime()) {
						double remainingDistanceshortPart=remainingDistance(0,partsList);
						double currentDistDepot=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, inp.getNodes().get(0).getId()-1)*(1+test.getDetour());
						if(remainingDistanceshortPart<=currentDistDepot) {// detour long
							merging=true;
							insertingTailRoute(list,partsList,partsList.get(0).getListSubJobs().size()-2,0);
							if(!list.isEmpty()) {
								newPart.setListSubJobs(list, inp, test);
							}
						}
					}
				}
			}
		}
		return merging;
	}



	private boolean checkingDetour(Edge edgeLongPart, Edge edgeShortPart, ArrayList<Parts> partsList) {
		boolean feasible=false;
		if(edgeLongPart==null) {
			System.out.print("stop");
		}
		if(edgeShortPart==null) {
			System.out.print("stop");
		}
		SubJobs jA=edgeLongPart.getOrigin();
		SubJobs jB=edgeLongPart.getEnd();
		SubJobs j1=edgeShortPart.getOrigin();
		SubJobs j2=edgeShortPart.getEnd();
		String key=jA.getSubJobKey()+jB.getSubJobKey();
		Edge connectionLarge=partsList.get(0).getDirectoryConnections().get(key);
		key=j1.getSubJobKey()+j2.getSubJobKey();
		Edge connectionShort=null;
		if(!partsList.get(1).getDirectoryConnections().isEmpty()) {
			connectionShort=partsList.get(1).getDirectoryConnections().get(key);			
		}
		else {
			connectionShort=new Edge(j1,j2,inp,test);
		}
		double distJAj1JB=inp.getCarCost().getCost(jA.getId()-1, j1.getId()-1)+inp.getCarCost().getCost(j1.getId()-1, jB.getId()-1);
		double distJ1jBJ2=inp.getCarCost().getCost(j1.getId()-1, jB.getId()-1)+inp.getCarCost().getCost(jB.getId()-1, j2.getId()-1);
		if(distJAj1JB<=connectionLarge.getDetour() && distJ1jBJ2<=connectionShort.getDetour()) {
			feasible=true;
		}
		return feasible;
	}

	private void insertingTailRoute(ArrayList<SubJobs> list, ArrayList<Parts> partsList, int i2, int j2) {
		for(int i=i2;i<partsList.get(0).getListSubJobs().size();i++) {
			if(i<partsList.get(0).getListSubJobs().size()-1) {
				list.add(partsList.get(0).getListSubJobs().get(i));
			}
			else {
				list.add(partsList.get(1).getListSubJobs().get(j2));
				list.add(partsList.get(0).getListSubJobs().get(i));
				for(int j=j2+1;j<partsList.get(1).getListSubJobs().size();j++) {
					list.add(partsList.get(1).getListSubJobs().get(j));
				}
			}

		}

	}

	private double remainingDistance(int j, ArrayList<Parts> partsList) {
		double distance=0;
		SubJobs lastLarge=partsList.get(0).getListSubJobs().get(partsList.get(0).getListSubJobs().size()-1);
		SubJobs startShort=partsList.get(1).getListSubJobs().get(j+1);
		double distNewConnectionLargeShort=inp.getCarCost().getCost(lastLarge.getId()-1, startShort.getId()-1);
		double distConnectionDepot=inp.getCarCost().getCost(partsList.get(1).getListSubJobs().get(partsList.get(1).getListSubJobs().size()-1).getId()-1, inp.getNodes().get(0).getId()-1);
		double distPart=0;
		for(int k=j+1;k<partsList.get(1).getListSubJobs().size()-1;k++) {
			String key=partsList.get(1).getListSubJobs().get(k).getSubJobKey()+partsList.get(1).getListSubJobs().get(k+1).getSubJobKey();
			Edge partEdge=partsList.get(1).getDirectoryConnections().get(key);
			distPart+=partEdge.getTime();
		}
		distance=distNewConnectionLargeShort+distPart+distConnectionDepot;
		return distance;
	}

	private ArrayList<Parts> selectingParts(Route earlyRoute, Route lateRoute) {
		ArrayList<Parts> partsList= new ArrayList<Parts>();
		int lengthEarly=earlyRoute.getPartsRoute().size()-1;
		int lengthLate=lateRoute.getPartsRoute().size()-1;
		if(earlyRoute.getPartsRoute().get(lengthEarly-1).getListSubJobs().size()>=lateRoute.getPartsRoute().get(lengthLate-1).getListSubJobs().size()) {
			partsList.add(0,earlyRoute.getPartsRoute().get(lengthEarly-1));
			partsList.add(lateRoute.getPartsRoute().get(lengthLate-1));
		}
		else {
			partsList.add(0,lateRoute.getPartsRoute().get(lengthLate-1));
			partsList.add(earlyRoute.getPartsRoute().get(lengthEarly-1));
		}
		return partsList;
	}

	private void updatingLateRoute(Route lateRoute, Route x, Route newRoute) {

		HashMap<String,Parts> listSubJobs=new HashMap<String,Parts>(); // copy of current list Jobs
		HashMap<String,SubJobs> newSubJobs=new HashMap<String,SubJobs>(); // new list Jobs
		for(Parts p:newRoute.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {
				if(j.getId()!=1) {
					newSubJobs.put(j.getSubJobKey(),j);}
			}
		}

		for(Parts j:lateRoute.getPartsRoute()) {
			listSubJobs.put(j.getKey(),j);
		}

		for(Parts p:listSubJobs.values()) {
			boolean removePart=false;
			for(SubJobs s:p.getListSubJobs()) {
				if(newSubJobs.containsKey(s.getSubJobKey())) {
					removePart=true;
				}
				if(removePart) {
					break;
				}
			}
			if(removePart) {
				removePart=false;
				lateRoute.getPartsRoute().remove(p);
				break;
			}
		}
		lateRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		for(Parts p:lateRoute.getPartsRoute()) {
			if(p.getListSubJobs().isEmpty()) {
				//System.out.println("stop");
			}
			p.settingConnections(p.getListSubJobs(),inp,test);
			if(p.getDirectoryConnections().size()==100) {
				//System.out.println("Stop");
			}
		}

	}

	private void updatingRefRoute(Route earlyRoute, Route newRoute) {
		earlyRoute.getSubJobsList().clear();
		//		for(SubJobs s:newRoute.getSubJobsList()) {
		//			earlyRoute.getSubJobsList().add(s);
		//		}
		for(Parts p:newRoute.getPartsRoute()) {
			for(SubJobs s:p.getListSubJobs()) {
				if(s.getId()!=1) {
					earlyRoute.getSubJobsList().add(s);
				}
			}
		}

		earlyRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		//earlyRoute.updateRouteFromSubJobsList(inp,test);
		for(Parts p:earlyRoute.getPartsRoute()) {
			p.settingConnections(p.getListSubJobs(),inp,test);
		}
	}

	private boolean settingIntermediateParts(Route earlyRoute, Route lateRoute, Route newRoute) {
		// este método no esta definido. Esta provisional
		// 1. Es necesario indetificar el tipo de partes a mezclar

		boolean merging=false;
		// empieza a mezclar desde la parte 2 de las rutas
		for(int partEarly=2; partEarly<(earlyRoute.getPartsRoute().size()-2);partEarly++) {
			System.out.print("Size"+ (lateRoute.getPartsRoute().size()-1));
			for(int partLate=2; partLate<(lateRoute.getPartsRoute().size()-2);partLate++) {
				System.out.print("Current partt"+ partLate);
				if(lateRoute.getIdRoute()==5 && earlyRoute.getIdRoute()==0 && partEarly==2 && partLate==4 ) {
					//System.out.println("early \n");
				}
				if(partEarly==4 && partLate==2 ) {
					//System.out.println("early \n");
				}
				int options=typeOfParts(earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate)); 

				switch(options) {
				case 1 :// 1 patient-patient 
					//System.out.println("early ID\n"+earlyRoute.getIdRoute());
					//System.out.println("early \n"+earlyRoute);
					//System.out.println("late ID\n"+lateRoute.getIdRoute());
					//System.out.println("late \n"+lateRoute);
					if(lateRoute.getIdRoute()==5 && earlyRoute.getIdRoute()==0 && partEarly==2 && partLate==4 ) {
						//System.out.println("early \n");
					}

					merging=mergingItermediatePartsPatientPatient(options,earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate), newRoute,earlyRoute,lateRoute);// option1
					break;

				case 2 : // 2 homeCare-patient or patient-homeCare
					merging=mergingItermediatePartsPatientClient(options,earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate), newRoute);// option1

					break; // optional

				default : // 3 homeCare-homeCare

					merging=mergingItermediatePartsClients(earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate), newRoute);// option1
				}
			}	
		}

		for(int i=2;i<earlyRoute.getPartsRoute().size()-2;i++) {
			Parts p=earlyRoute.getPartsRoute().get(i);
			for(SubJobs s:p.getListSubJobs()) {
				if(!newRoute.getSubJobsList().contains(s)) {
					newRoute.getSubJobsList().add(s);
				}}
		}
		//	newRoute.getSubJobsList().add(earlyRoute.getPartsRoute().getLast().getListSubJobs().get(0));
		return merging;
	}

	//	private boolean settingIntermediateParts(Route earlyRoute, Route lateRoute, Route newRoute) {
	//		// este método no esta definido. Esta provisional
	//		// 1. Es necesario indetificar el tipo de partes a mezclar
	//
	//		boolean merging=false;
	//		// empieza a mezclar desde la parte 2 de las rutas
	//		for(int partEarly=2; partEarly<(earlyRoute.getPartsRoute().size()-2);partEarly++) {
	//			for(int partLate=2; partLate<(lateRoute.getPartsRoute().size()-2);partLate++) {
	//				mergingPart(earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate),newRoute,merging);
	//
	//
	//			}	
	//		}
	//
	//		for(int i=2;i<earlyRoute.getPartsRoute().size()-2;i++) {
	//			Parts p=earlyRoute.getPartsRoute().get(i);
	//			for(SubJobs s:p.getListSubJobs()) {
	//				if(!newRoute.getSubJobsList().contains(s)) {
	//					newRoute.getSubJobsList().add(s);
	//				}}
	//		}
	//		//	newRoute.getSubJobsList().add(earlyRoute.getPartsRoute().getLast().getListSubJobs().get(0));
	//		return merging;
	//	}

	private void mergingPart(Parts pref, Parts psplit, Route newRoute, boolean merging) {
		ArrayList<SubJobs> jobListPars= selectingJobs(pref,psplit);
		ArrayList<SubJobs>	newList= new ArrayList<SubJobs>();
		for(int i=1;i<newRoute.getPartsRoute().size();i++) {
			Parts p=newRoute.getPartsRoute().get(i);
			for(SubJobs j:p.getListSubJobs()) {
				newList.add(j);

			}
		}
		for(SubJobs j:jobListPars) {
			newList.add(j);
		}
		Parts newPart= new Parts();
		newPart.setListSubJobs(jobListPars, inp, test);
		newPart.settingConnections( pref,  psplit);
		if(vehicleCapacityPart(newList)) {
			merging=true;

			newRoute.getPartsRoute().add(newPart);
			newRoute.getSubJobsList().clear();
			newRoute.getJobsDirectory().clear();
			for(SubJobs j:newList) {
				newRoute.getSubJobsList().add(j);
				newRoute.getJobsDirectory().put(j.getSubJobKey(), j);
			}
		}
	}

	private ArrayList<SubJobs> selectingJobs(Parts pref, Parts psplit) {
		ArrayList<SubJobs> list= new ArrayList<SubJobs>();
		for(SubJobs j:pref.getListSubJobs()) {
			list.add(j);
		}
		for(SubJobs j:psplit.getListSubJobs()) {
			list.add(j);
		}
		list.sort(SubJobs.SORT_BY_ENDTW);
		return list;
	}

	private boolean mergingItermediatePartsPatientClient(int options, Parts earlyRoute, Parts lateRoute, Route newRoute) {
		// structure (pik-up)---(drop-off)---(pik-up)---(drop-off)
		boolean merge=false;
		ArrayList<Parts> ordering=orderingParts(earlyRoute,lateRoute);
		merge=notequalMerge(ordering,newRoute);
		if(!merge) {
			merge=notequalMergeInfesible(ordering,newRoute);
		}
		return merge;
	}

	private boolean notequalMergeInfesible(ArrayList<Parts> ordering, Route newRoute) {
		boolean merging= false;
		//
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		// list of jobs - Late
		SubJobs pickUp1Late=ordering.get(1).getListSubJobs().get(0);
		SubJobs dropOff1Late=ordering.get(1).getListSubJobs().get(1);
		for(SubJobs s:ordering.get(0).getListSubJobs()) { // (ordering.get(1))---(ordering.get(0))---(ordering.get(1))

			double tvPickUp1LatePickUp1Early=inp.getCarCost().getCost(pickUp1Late.getId()-1, s.getId()-1);
			//boolean waintingTime=checkWaitingTime(s,tvPickUp1LatePickUp1Early,pickUp1Late);
			if(pickUp1Late.getDepartureTime()<=s.getArrivalTime()) {// (ordering.get(1))---(ordering.get(0))
				String key="";
				//boolean feasibledetour=checkFeasibilityDetour(s,ordering.get(0),pickUp1Late);
				//if(feasibledetour) { // check detour
				for(SubJobs next:ordering.get(0).getListSubJobs()) { // trying to insert the next part (ordering.get(0))---(ordering.get(1))
					double tvnextdropOff1Late= inp.getCarCost().getCost(next.getId(), dropOff1Late.getId());
					if(next.getDepartureTime()<=dropOff1Late.getArrivalTime()) {// (ordering.get(0))---(ordering.get(1))
						// home care
						double detour=computingdetour(s,next,ordering.get(0));
						double detourDuration=detour+tvPickUp1LatePickUp1Early+tvnextdropOff1Late;
						key=ordering.get(1).getListSubJobs().get(0).getSubJobKey()+ordering.get(1).getListSubJobs().get(1).getSubJobKey();
						Edge connection=ordering.get(1).getDirectoryConnections().get(key);
						// patient home
						int lastJob=ordering.get(0).getListSubJobs().size()-1;
						if(!next.equals(ordering.get(0).getListSubJobs().get(lastJob))) {
							int a=ordering.get(0).getListSubJobs().indexOf(next);
							SubJobs afterJob=ordering.get(0).getListSubJobs().get(a+1);
							//feasibledetour=checkFeasibilityDetour(afterJob,ordering.get(0),pickUp1Late);
						}
						//							else { // next is the last subJobs
						//								feasibledetour=true;
						//							}
						//if(detourDuration<=connection.getDetour() && feasibledetour) { // checking detour

						for(SubJobs jS:ordering.get(0).getListSubJobs()) {
							if(jS.equals(s) || next.equals(s)) {
								if(jS.equals(s) ) {
									listSubJobs.add(pickUp1Late);
									listSubJobs.add(jS);
									//											newRoute.getSubJobsList().add(pickUp1Late);
									//											newRoute.getSubJobsList().add(jS);
								}
								else if(next.equals(s)) {
									listSubJobs.add(jS);
									listSubJobs.add(dropOff1Late);
									//											newRoute.getSubJobsList().add(jS);
									//											newRoute.getSubJobsList().add(dropOff1Late);
								}
							}
							else {
								listSubJobs.add(jS);
							}
						}
						if(vehicleCapacityPart(listSubJobs)) {
							merging=true;
							newPart.setListSubJobs(listSubJobs, inp, test);
							newRoute.getPartsRoute().add(newPart);
							newPart.settingConnections( ordering.get(0),  ordering.get(1));
							newRoute.getPartsRoute().add(newPart);
							break;}
						//}
					}
					if(merging) {
						break;
					}
				}
				//}
			}
		}
		return merging;
	}

	private boolean changeTimePatientClient(ArrayList<Parts> ordering, Route newRoute) {
		boolean merge=false;

		return merge;
	}

	private boolean notequalMerge(ArrayList<Parts> ordering, Route newRoute) {
		boolean merging= false;
		//
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		// list of jobs - Late
		SubJobs pickUp1Late=ordering.get(1).getListSubJobs().get(0);
		SubJobs dropOff1Late=ordering.get(1).getListSubJobs().get(1);
		for(SubJobs s:ordering.get(0).getListSubJobs()) { // (ordering.get(1))---(ordering.get(0))---(ordering.get(1))

			double tvPickUp1LatePickUp1Early=inp.getCarCost().getCost(pickUp1Late.getId()-1, s.getId()-1);
			boolean waintingTime=checkWaitingTime(s,tvPickUp1LatePickUp1Early,pickUp1Late);
			if(pickUp1Late.getDepartureTime()+tvPickUp1LatePickUp1Early<=s.getArrivalTime() && waintingTime) {// (ordering.get(1))---(ordering.get(0))
				String key="";
				boolean feasibledetour=checkFeasibilityDetour(s,ordering.get(0),pickUp1Late);
				if(feasibledetour) { // check detour
					for(SubJobs next:ordering.get(0).getListSubJobs()) { // trying to insert the next part (ordering.get(0))---(ordering.get(1))
						double tvnextdropOff1Late= inp.getCarCost().getCost(next.getId(), dropOff1Late.getId());
						if(next.getDepartureTime()+tvnextdropOff1Late<=dropOff1Late.getArrivalTime()) {// (ordering.get(0))---(ordering.get(1))
							// home care
							double detour=computingdetour(s,next,ordering.get(0));
							double detourDuration=detour+tvPickUp1LatePickUp1Early+tvnextdropOff1Late;
							key=ordering.get(1).getListSubJobs().get(0).getSubJobKey()+ordering.get(1).getListSubJobs().get(1).getSubJobKey();
							Edge connection=ordering.get(1).getDirectoryConnections().get(key);
							// patient home
							int lastJob=ordering.get(0).getListSubJobs().size()-1;
							if(!next.equals(ordering.get(0).getListSubJobs().get(lastJob))) {
								int a=ordering.get(0).getListSubJobs().indexOf(next);
								SubJobs afterJob=ordering.get(0).getListSubJobs().get(a+1);
								feasibledetour=checkFeasibilityDetour(afterJob,ordering.get(0),pickUp1Late);
							}
							else { // next is the last subJobs
								feasibledetour=true;
							}
							if(detourDuration<=connection.getDetour() && feasibledetour) { // checking detour

								for(SubJobs jS:ordering.get(0).getListSubJobs()) {
									if(jS.equals(s) || next.equals(s)) {
										if(jS.equals(s) ) {
											listSubJobs.add(pickUp1Late);
											listSubJobs.add(jS);
											//											newRoute.getSubJobsList().add(pickUp1Late);
											//											newRoute.getSubJobsList().add(jS);
										}
										else if(next.equals(s)) {
											listSubJobs.add(jS);
											listSubJobs.add(dropOff1Late);
											//											newRoute.getSubJobsList().add(jS);
											//											newRoute.getSubJobsList().add(dropOff1Late);
										}
									}
									else {
										listSubJobs.add(jS);
									}
								}
								if(vehicleCapacityPart(listSubJobs)) {
									merging=true;
									newPart.setListSubJobs(listSubJobs, inp, test);
									newRoute.getPartsRoute().add(newPart);
									newPart.settingConnections( ordering.get(0),  ordering.get(1));
									newRoute.getPartsRoute().add(newPart);
									break;}
							}
						}
						if(merging) {
							break;
						}
					}
				}
			}
		}
		return merging;
	}



	private boolean checkFeasibilityDetour(SubJobs s, Parts parts, SubJobs pickUp1Late) {
		boolean feasibledetour=false;
		double detourPart1=0;
		String key="";
		int i=parts.getListSubJobs().indexOf(s);

		if(i!=0 &&!s.equals(parts.getListSubJobs().get(parts.getListSubJobs().size()-1))) {

			key=parts.getListSubJobs().get(i-1).getSubJobKey()+parts.getListSubJobs().get(i).getSubJobKey();
			Edge connection=parts.getDirectoryConnections().get(key);
			detourPart1=computingDetour(s,parts,pickUp1Late);		
			if(detourPart1<=connection.getDetour()) {
				feasibledetour=true;
			}
		}
		else {feasibledetour=true;}
		return feasibledetour;
	}

	private double computingDetour(SubJobs s, Parts parts, SubJobs pickUp1Late) {
		double detour=0;
		if(!s.equals(parts.getListSubJobs().get(0))) {
			int a=parts.getListSubJobs().indexOf(s);
			SubJobs j=parts.getListSubJobs().get(a-1);
			SubJobs i=parts.getListSubJobs().get(a);
			detour=inp.getCarCost().getCost(i.getId()-1, pickUp1Late.getId()-1)+inp.getCarCost().getCost(pickUp1Late.getId()-1,j.getId()-1);
		}
		return detour;
	}

	private double computingdetour(SubJobs s, SubJobs next, Parts parts) {
		double detour=0;
		int a=parts.getListSubJobs().indexOf(s);
		for(int i=a;i<parts.getListSubJobs().size();i++) {
			SubJobs jobs=parts.getListSubJobs().get(i);
			if(next.equals(jobs)) {		
				break;
			}
			else {
				if(i+1<parts.getListSubJobs().size()) {
					SubJobs jobsNext=parts.getListSubJobs().get(i+1);
					detour=inp.getCarCost().getCost(jobs.getId()-1, jobsNext.getId()-1);}
			}
		}
		return detour;
	}

	private boolean mergingItermediatePartsPatientPatient(int options, Parts early, Parts late, Route newRoute, Route earlyRoute, Route lateRoute) {
		boolean merging=keepPatientPatient(options,early,late,newRoute,earlyRoute,lateRoute);
		if(!merging) {
			merging=keepPatientPatientInfeasible(options,early,late,newRoute,earlyRoute,lateRoute);}
		return merging;
	}

	private boolean keepPatientPatientInfeasible(int options, Parts early, Parts late, Route newRoute, Route earlyRoute,
			Route lateRoute) {

		boolean merging=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		if(early.getListSubJobs().size()==late.getListSubJobs().size() && late.getListSubJobs().size()==4) {
			/* part structure: (pick patient up at medical centre)
		(drop patient off at patient home)
		(pick next patient up at home)
		(drop next patient off at at medical centre)
			 */
			// early part

			SubJobs earlypickMC=early.getListSubJobs().get(0);
			SubJobs earlydropOffPatientHome=early.getListSubJobs().get(1);
			SubJobs earlypickUpPatientHome=early.getListSubJobs().get(2);
			SubJobs earlydropOffMedicalCentre=early.getListSubJobs().get(3);

			// late part
			SubJobs latepickMC=late.getListSubJobs().get(0);
			SubJobs latedropOffPatientHome=late.getListSubJobs().get(1);
			SubJobs latepickUpPatientHome=late.getListSubJobs().get(2);
			SubJobs latedropOffMedicalCentre=late.getListSubJobs().get(3);
			/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)
			 */
			boolean mergingPickUp= checkingTWDetourInfeasible(earlypickMC,latepickMC,earlydropOffPatientHome,latedropOffPatientHome,early,late); // checking: (early pick medical centre)---(late pick up medica centre)---(early drop off patient home)---(late drop off patient home)--
			if(mergingPickUp) { //
				mergingPickUp= checkingTWDetourInfeasible(earlypickUpPatientHome,latepickUpPatientHome,earlydropOffMedicalCentre,latedropOffMedicalCentre,early,late); // checking: (early pick medical centre)---(late pick medical centre)---(early drop off medical centre)---(late drop off medical centre)
				if(mergingPickUp) {
					listSubJobs.add(earlypickMC);
					listSubJobs.add(latepickMC);
					listSubJobs.add(earlydropOffPatientHome);
					listSubJobs.add(latedropOffPatientHome);
					listSubJobs.add(earlypickUpPatientHome);
					listSubJobs.add(latepickUpPatientHome);
					listSubJobs.add(earlydropOffMedicalCentre);
					listSubJobs.add(latedropOffMedicalCentre);
					mergingPickUp=vehicleCapacityPart(listSubJobs);
					if(mergingPickUp) {
						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections( early,  late);
						newRoute.getPartsRoute().add(newPart);}

					//				newRoute.getSubJobsList().add(earlypickMC);
					//				newRoute.getSubJobsList().add(latepickMC);
					//				newRoute.getSubJobsList().add(earlydropOffPatientHome);
					//				newRoute.getSubJobsList().add(latedropOffPatientHome);
					//				newRoute.getSubJobsList().add(earlypickUpPatientHome);
					//				newRoute.getSubJobsList().add(latepickUpPatientHome);
					//				newRoute.getSubJobsList().add(earlydropOffMedicalCentre);
					//				newRoute.getSubJobsList().add(latedropOffMedicalCentre);
				}
			}
			else {
				/*Option 2 
		(early pick patient up at medical centre)---(early drop patient off at patient home)
		(late pick patient up at medical centre)---(late drop patient off at patient home)
		(early pick next patient up at home)---(early drop patient off at medical centre)
		(late pick next patient up at home)---(late drop patient off at medical centre) 
				 */
				double distDropOffPickEarlyLate= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latepickMC.getId()-1); // (early drop patient off at patient home)---(late pick patient up at medical centre)
				// evaluate option 2: en la opción dos no se consideran los detours
				//boolean waintingTime=checkWaitingTime(latepickMC,distDropOffPickEarlyLate,earlydropOffPatientHome);
				if(earlydropOffPatientHome.getDepartureTime()+distDropOffPickEarlyLate<=latepickMC.getArrivalTime() ) { // checking TW: (early drop patient off at patient home)---(late pick patient up at medical centre)	
					double distDropOffPickLateEarly=inp.getCarCost().getCost(latedropOffPatientHome.getId()-1, earlypickUpPatientHome.getId()-1);// (late drop patient off at patient home)---(early pick next patient up at home)
					//waintingTime=checkWaitingTime(earlypickUpPatientHome,distDropOffPickLateEarly,latedropOffPatientHome);
					if(latedropOffPatientHome.getDepartureTime()+distDropOffPickLateEarly<=earlypickUpPatientHome.getArrivalTime()) { // check TW: (late drop patient off at patient home)---(early pick next patient up at home)
						double distDropOffPickEaryLate=inp.getCarCost().getCost(earlydropOffMedicalCentre.getId()-1, latepickUpPatientHome.getId()-1);
						//waintingTime=checkWaitingTime(latepickUpPatientHome,distDropOffPickEaryLate,earlydropOffMedicalCentre);
						if(earlydropOffMedicalCentre.getDepartureTime()+distDropOffPickEaryLate<=latepickUpPatientHome.getArrivalTime()) {// check TW: (early drop patient off at medical centre)---(late pick next patient up at home)

							listSubJobs.add(earlypickMC);
							listSubJobs.add(earlydropOffPatientHome);
							listSubJobs.add(latepickMC);
							listSubJobs.add(latedropOffPatientHome);
							listSubJobs.add(earlypickUpPatientHome);
							listSubJobs.add(earlydropOffMedicalCentre);
							listSubJobs.add(latepickUpPatientHome);
							listSubJobs.add(latedropOffMedicalCentre);
							if(vehicleCapacityPart(listSubJobs)) {
								mergingPickUp=true;
								newPart.setListSubJobs(listSubJobs, inp, test);
								newRoute.getPartsRoute().add(newPart);
								newPart.settingConnections( early,  late);
								newRoute.getPartsRoute().add(newPart);}
						}
					}
				}
			}
		}
		else {
			merging=mergingItermediatePartsPatientClient(options,early,late, newRoute);
		}// option1
		return merging;
	}

	private boolean changePatientPatient(int options, Parts early, Parts late, Route newRoute, Route earlyRoute,
			Route lateRoute) {
		boolean merging=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		/* part structure: (pick patient up at medical centre)
		(drop patient off at patient home)
		(pick next patient up at home)
		(drop next patient off at at medical centre)
		 */
		// early part

		SubJobs earlypickMC=early.getListSubJobs().get(0);
		SubJobs earlydropOffPatientHome=early.getListSubJobs().get(1);
		SubJobs earlypickUpPatientHome=early.getListSubJobs().get(2);
		SubJobs earlydropOffMedicalCentre=early.getListSubJobs().get(3);

		// late part
		SubJobs latepickMC=late.getListSubJobs().get(0);
		SubJobs latedropOffPatientHome=late.getListSubJobs().get(1);
		SubJobs latepickUpPatientHome=late.getListSubJobs().get(2);
		SubJobs latedropOffMedicalCentre=late.getListSubJobs().get(3);
		/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)
		 */
		boolean mergingPickUp= changingTWDetour(earlypickMC,latepickMC,earlydropOffPatientHome,latedropOffPatientHome,early,late); // checking: (early pick medical centre)---(late pick up medica centre)---(early drop off patient home)---(late drop off patient home)--
		if(mergingPickUp) { //
			mergingPickUp= checkingTWDetour(earlypickUpPatientHome,latepickUpPatientHome,earlydropOffMedicalCentre,latedropOffMedicalCentre,early,late); // checking: (early pick medical centre)---(late pick medical centre)---(early drop off medical centre)---(late drop off medical centre)
			if(mergingPickUp) {
				listSubJobs.add(earlypickMC);
				listSubJobs.add(latepickMC);
				listSubJobs.add(earlydropOffPatientHome);
				listSubJobs.add(latedropOffPatientHome);
				listSubJobs.add(earlypickUpPatientHome);
				listSubJobs.add(latepickUpPatientHome);
				listSubJobs.add(earlydropOffMedicalCentre);
				listSubJobs.add(latedropOffMedicalCentre);
				newPart.setListSubJobs(listSubJobs, inp, test);
				newRoute.getPartsRoute().add(newPart);
				newPart.settingConnections( early,  late);
				newRoute.getPartsRoute().add(newPart);
			}
		}
		else {
			/*Option 2 
		(early pick patient up at medical centre)---(early drop patient off at patient home)
		(late pick patient up at medical centre)---(late drop patient off at patient home)
		(early pick next patient up at home)---(early drop patient off at medical centre)
		(late pick next patient up at home)---(late drop patient off at medical centre) 
			 */
			double distDropOffPickEarlyLate= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latepickMC.getId()-1); // (early drop patient off at patient home)---(late pick patient up at medical centre)
			// evaluate option 2: en la opción dos no se consideran los detours
			boolean waintingTime=checkWaitingTime(latepickMC,distDropOffPickEarlyLate,earlydropOffPatientHome);
			if(earlydropOffPatientHome.getDepartureTime()+distDropOffPickEarlyLate<=latepickMC.getArrivalTime() && waintingTime) { // checking TW: (early drop patient off at patient home)---(late pick patient up at medical centre)	
				double distDropOffPickLateEarly=inp.getCarCost().getCost(latedropOffPatientHome.getId()-1, earlypickUpPatientHome.getId()-1);// (late drop patient off at patient home)---(early pick next patient up at home)
				waintingTime=checkWaitingTime(earlypickUpPatientHome,distDropOffPickLateEarly,latedropOffPatientHome);
				if(latedropOffPatientHome.getDepartureTime()+distDropOffPickLateEarly<=earlypickUpPatientHome.getArrivalTime() && waintingTime) { // check TW: (late drop patient off at patient home)---(early pick next patient up at home)
					double distDropOffPickEaryLate=inp.getCarCost().getCost(earlydropOffMedicalCentre.getId()-1, latepickUpPatientHome.getId()-1);
					waintingTime=checkWaitingTime(latepickUpPatientHome,distDropOffPickEaryLate,earlydropOffMedicalCentre);
					if(earlydropOffMedicalCentre.getDepartureTime()+distDropOffPickEaryLate<=latepickUpPatientHome.getArrivalTime() && waintingTime) {// check TW: (early drop patient off at medical centre)---(late pick next patient up at home)
						mergingPickUp=true;
						listSubJobs.add(earlypickMC);
						listSubJobs.add(earlydropOffPatientHome);
						listSubJobs.add(latepickMC);
						listSubJobs.add(latedropOffPatientHome);
						listSubJobs.add(earlypickUpPatientHome);
						listSubJobs.add(earlydropOffMedicalCentre);
						listSubJobs.add(latepickUpPatientHome);
						listSubJobs.add(latedropOffMedicalCentre);
						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections( early,  late);
						newRoute.getPartsRoute().add(newPart);

					}
				}
			}
		}
		return merging;}

	private boolean keepPatientPatient(int options, Parts early, Parts late, Route newRoute, Route earlyRoute,
			Route lateRoute) {
		boolean merging=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		if(early.getListSubJobs().size()==late.getListSubJobs().size() && late.getListSubJobs().size()==4) {
			/* part structure: (pick patient up at medical centre)
		(drop patient off at patient home)
		(pick next patient up at home)
		(drop next patient off at at medical centre)
			 */
			// early part

			SubJobs earlypickMC=early.getListSubJobs().get(0);
			SubJobs earlydropOffPatientHome=early.getListSubJobs().get(1);
			SubJobs earlypickUpPatientHome=early.getListSubJobs().get(2);
			SubJobs earlydropOffMedicalCentre=early.getListSubJobs().get(3);

			// late part
			SubJobs latepickMC=late.getListSubJobs().get(0);
			SubJobs latedropOffPatientHome=late.getListSubJobs().get(1);
			SubJobs latepickUpPatientHome=late.getListSubJobs().get(2);
			SubJobs latedropOffMedicalCentre=late.getListSubJobs().get(3);
			/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)
			 */
			boolean mergingPickUp= checkingTWDetour(earlypickMC,latepickMC,earlydropOffPatientHome,latedropOffPatientHome,early,late); // checking: (early pick medical centre)---(late pick up medica centre)---(early drop off patient home)---(late drop off patient home)--
			if(mergingPickUp) { //
				mergingPickUp= checkingTWDetour(earlypickUpPatientHome,latepickUpPatientHome,earlydropOffMedicalCentre,latedropOffMedicalCentre,early,late); // checking: (early pick medical centre)---(late pick medical centre)---(early drop off medical centre)---(late drop off medical centre)
				if(mergingPickUp) {
					listSubJobs.add(earlypickMC);
					listSubJobs.add(latepickMC);
					listSubJobs.add(earlydropOffPatientHome);
					listSubJobs.add(latedropOffPatientHome);
					listSubJobs.add(earlypickUpPatientHome);
					listSubJobs.add(latepickUpPatientHome);
					listSubJobs.add(earlydropOffMedicalCentre);
					listSubJobs.add(latedropOffMedicalCentre);
					mergingPickUp=vehicleCapacityPart(listSubJobs);
					if(mergingPickUp) {
						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections( early,  late);
						newRoute.getPartsRoute().add(newPart);}

					//				newRoute.getSubJobsList().add(earlypickMC);
					//				newRoute.getSubJobsList().add(latepickMC);
					//				newRoute.getSubJobsList().add(earlydropOffPatientHome);
					//				newRoute.getSubJobsList().add(latedropOffPatientHome);
					//				newRoute.getSubJobsList().add(earlypickUpPatientHome);
					//				newRoute.getSubJobsList().add(latepickUpPatientHome);
					//				newRoute.getSubJobsList().add(earlydropOffMedicalCentre);
					//				newRoute.getSubJobsList().add(latedropOffMedicalCentre);
				}
			}
			else {
				/*Option 2 
		(early pick patient up at medical centre)---(early drop patient off at patient home)
		(late pick patient up at medical centre)---(late drop patient off at patient home)
		(early pick next patient up at home)---(early drop patient off at medical centre)
		(late pick next patient up at home)---(late drop patient off at medical centre) 
				 */
				double distDropOffPickEarlyLate= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latepickMC.getId()-1); // (early drop patient off at patient home)---(late pick patient up at medical centre)
				// evaluate option 2: en la opción dos no se consideran los detours
				boolean waintingTime=checkWaitingTime(latepickMC,distDropOffPickEarlyLate,earlydropOffPatientHome);
				if(earlydropOffPatientHome.getDepartureTime()+distDropOffPickEarlyLate<=latepickMC.getArrivalTime() && waintingTime) { // checking TW: (early drop patient off at patient home)---(late pick patient up at medical centre)	
					double distDropOffPickLateEarly=inp.getCarCost().getCost(latedropOffPatientHome.getId()-1, earlypickUpPatientHome.getId()-1);// (late drop patient off at patient home)---(early pick next patient up at home)
					waintingTime=checkWaitingTime(earlypickUpPatientHome,distDropOffPickLateEarly,latedropOffPatientHome);
					if(latedropOffPatientHome.getDepartureTime()+distDropOffPickLateEarly<=earlypickUpPatientHome.getArrivalTime() && waintingTime) { // check TW: (late drop patient off at patient home)---(early pick next patient up at home)
						double distDropOffPickEaryLate=inp.getCarCost().getCost(earlydropOffMedicalCentre.getId()-1, latepickUpPatientHome.getId()-1);
						waintingTime=checkWaitingTime(latepickUpPatientHome,distDropOffPickEaryLate,earlydropOffMedicalCentre);
						if(earlydropOffMedicalCentre.getDepartureTime()+distDropOffPickEaryLate<=latepickUpPatientHome.getArrivalTime() && waintingTime) {// check TW: (early drop patient off at medical centre)---(late pick next patient up at home)

							listSubJobs.add(earlypickMC);
							listSubJobs.add(earlydropOffPatientHome);
							listSubJobs.add(latepickMC);
							listSubJobs.add(latedropOffPatientHome);
							listSubJobs.add(earlypickUpPatientHome);
							listSubJobs.add(earlydropOffMedicalCentre);
							listSubJobs.add(latepickUpPatientHome);
							listSubJobs.add(latedropOffMedicalCentre);
							if(vehicleCapacityPart(listSubJobs)) {
								mergingPickUp=true;
								newPart.setListSubJobs(listSubJobs, inp, test);
								newRoute.getPartsRoute().add(newPart);
								newPart.settingConnections( early,  late);
								newRoute.getPartsRoute().add(newPart);}
							//						newRoute.getSubJobsList().add(earlypickMC);
							//						newRoute.getSubJobsList().add(earlydropOffPatientHome);
							//						newRoute.getSubJobsList().add(latepickMC);
							//						newRoute.getSubJobsList().add(latedropOffPatientHome);
							//						newRoute.getSubJobsList().add(earlypickUpPatientHome);
							//						newRoute.getSubJobsList().add(earlydropOffMedicalCentre);
							//						newRoute.getSubJobsList().add(latepickUpPatientHome);
							//						newRoute.getSubJobsList().add(latedropOffMedicalCentre);
						}
					}
				}
			}
		}
		else {
			merging=mergingItermediatePartsPatientClient(options,early,late, newRoute);
		}// option1
		return merging;}

	private boolean changingTWDetour(SubJobs earlypickMC, SubJobs latepickMC, SubJobs earlydropOffPatientHome,
			SubJobs latedropOffPatientHome, Parts early, Parts late) {
		/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)*/
		boolean merge=false;
		double distpickUpDropOff=inp.getCarCost().getCost(latepickMC.getId()-1, earlydropOffPatientHome.getId()-1);
		//computing distances
		double distpickMedicalCentre= inp.getCarCost().getCost(earlypickMC.getId()-1, latepickMC.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		double distdropOffPatient= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		// changing times latepickMC
		double [] times= calculatingTimesLateEarly(earlydropOffPatientHome,latepickMC);  // departure=times[0];  arrival=times[1]; startServiceTime=times[2];
		// changing times latepickMC
		double [] timesEarlyLate= calculatingTimesEarlyLate(earlydropOffPatientHome,latedropOffPatientHome);  // departure=times[0];  arrival=times[1]; startServiceTime=times[2];

		if(times[2]>=latepickMC.getStartTime() && times[2]<=latepickMC.getEndTime() &&
				timesEarlyLate[2]>=latedropOffPatientHome.getStartTime() && timesEarlyLate[2]<=latedropOffPatientHome.getEndTime()	){
			if(times[0]>=earlydropOffPatientHome.getArrivalTime() && times[1]<=(earlypickMC.getDepartureTime()+distpickMedicalCentre) &&
					(earlydropOffPatientHome.getDepartureTime()+distdropOffPatient)<=timesEarlyLate[1]){
				String key=latepickMC.getSubJobKey()+latedropOffPatientHome.getSubJobKey();
				Edge rempovedConnectionLate=late.getDirectoryConnections().get(key);
				Edge rempovedConnectionearly=early.getDirectoryConnections().get(key);
				if((distpickMedicalCentre+distdropOffPatient)<=rempovedConnectionearly.getDetour() && (distpickUpDropOff+distdropOffPatient)<=rempovedConnectionLate.getDetour()) { // checking detour
					merge=true;
					latepickMC.setarrivalTime(times[1]);
					latepickMC.setStartServiceTime(times[2]);
					latepickMC.setEndServiceTime(times[2]+latepickMC.getReqTime());
					latepickMC.setdepartureTime(times[0]);
				}
			}	
		}

		return merge;
	}


	private double[] calculatingTimesEarlyLate(SubJobs earlydropOffPatientHome, SubJobs latedropOffPatientHome) {
		double [] times= new double [3];
		double loadUnloadTime=0;
		double registration=0;
		double tv=inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);
		if(latedropOffPatientHome.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=earlydropOffPatientHome.getDepartureTime()+tv;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		times[0]=departure;
		times[1]=arrival;
		times[2]=startServiceTime;
		return times;
	}

	private double[] calculatingTimesLateEarly(SubJobs earlydropOffPatientHome, SubJobs latepickMC) {
		double [] times= new double [3];
		double loadUnloadTime=0;
		double registration=0;
		double tv=inp.getCarCost().getCost(latepickMC.getId()-1, earlydropOffPatientHome.getId()-1);
		if(latepickMC.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=earlydropOffPatientHome.getArrivalTime()-tv;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		times[0]=departure;
		times[1]=arrival;
		times[2]=startServiceTime;
		return times;
	}

	private boolean checkingTWDetour(SubJobs earlypickMC, SubJobs latepickMC, SubJobs earlydropOffPatientHome,
			/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
			(early drop patient off at patient home) --- (late drop patient off at patient home)
			(early pick next patient up at home)---(late pick next patient up at home)
			(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)*/
			SubJobs latedropOffPatientHome, Parts early, Parts late) {
		boolean merge=false;
		double distpickUpDropOff=inp.getCarCost().getCost(latepickMC.getId()-1, earlydropOffPatientHome.getId()-1);
		double distDropOffDropOff=inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);
		//computing distances
		double distpickMedicalCentre= inp.getCarCost().getCost(earlypickMC.getId()-1, latepickMC.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		double distdropOffPatient= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		boolean waintingTime=checkWaitingTime(earlydropOffPatientHome,distpickUpDropOff,latedropOffPatientHome);
		if(latedropOffPatientHome.getDepartureTime()+distpickUpDropOff<earlydropOffPatientHome.getArrivalTime() && waintingTime) {// (early pick medical centre)---x---(early drop off patient home)
			waintingTime=checkWaitingTime(latedropOffPatientHome,distDropOffDropOff,earlydropOffPatientHome);
			if(earlydropOffPatientHome.getDepartureTime()+distDropOffDropOff<latedropOffPatientHome.getArrivalTime() && waintingTime) {//(late pick up medica centre)---x---(late drop off patient home)
				String key=latepickMC.getSubJobKey()+latedropOffPatientHome.getSubJobKey();
				Edge rempovedConnectionLate=late.getDirectoryConnections().get(key);
				Edge rempovedConnectionearly=early.getDirectoryConnections().get(key);
				if((distpickMedicalCentre+distdropOffPatient)<=rempovedConnectionearly.getDetour() && (distpickUpDropOff+distdropOffPatient)<=rempovedConnectionLate.getDetour()) { // checking detour
					merge=true;
				}
			}
		}
		return merge;
	}

	private boolean checkingTWDetourInfeasible(SubJobs earlypickMC, SubJobs latepickMC, SubJobs earlydropOffPatientHome,SubJobs latedropOffPatientHome, Parts early, Parts late) {
		/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)*/
		boolean merge=false;
		double distpickUpDropOff=inp.getCarCost().getCost(latepickMC.getId()-1, earlydropOffPatientHome.getId()-1);
		double distDropOffDropOff=inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);
		//computing distances
		double distpickMedicalCentre= inp.getCarCost().getCost(earlypickMC.getId()-1, latepickMC.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		double distdropOffPatient= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		//boolean waintingTime=checkWaitingTime(earlydropOffPatientHome,distpickUpDropOff,latedropOffPatientHome);
		if(latedropOffPatientHome.getDepartureTime()<earlydropOffPatientHome.getArrivalTime()) {// (early pick medical centre)---x---(early drop off patient home)
			//waintingTime=checkWaitingTime(latedropOffPatientHome,distDropOffDropOff,earlydropOffPatientHome);
			if(earlydropOffPatientHome.getDepartureTime()<latedropOffPatientHome.getArrivalTime()) {//(late pick up medica centre)---x---(late drop off patient home)
				String key=latepickMC.getSubJobKey()+latedropOffPatientHome.getSubJobKey();
				Edge rempovedConnectionLate=late.getDirectoryConnections().get(key);
				Edge rempovedConnectionearly=early.getDirectoryConnections().get(key);
				merge=true;

			}
		}
		return merge;
	}


	private boolean mergingItermediatePartsClients(Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging=false;
		if(earlyPart.getListSubJobs().size()==2 && latePart.getListSubJobs().size()==2 ) { // structure: (pick-up) --- (drop-off)
			merging=fourSubJobs(earlyPart,latePart,newRoute);
		}
		if((earlyPart.getListSubJobs().size()==1 && latePart.getListSubJobs().size()==2) || (latePart.getListSubJobs().size()==1 && earlyPart.getListSubJobs().size()==2) ) {
			merging=	threeSubJobs(earlyPart,latePart,newRoute);
		}
		return merging;
	}






	private boolean threeSubJobs(Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging=false;
		//
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		//
		ArrayList<Parts> ordering=orderingParts(earlyPart,latePart); //ordena de manera descendente las partes de acuerdo al número de subjobs contenidos en cada parte 
		SubJobs pickUpRoute1=ordering.get(0).getListSubJobs().get(0);
		SubJobs pickUpRoute2=ordering.get(1).getListSubJobs().get(0);
		SubJobs dropOffRoute2=ordering.get(1).getListSubJobs().get(1);
		/*Option 1: (pickUpRoute2)---(pickUpRoute1)---(dropOffRoute2)*/
		double distpickUpRoute2pickUpRoute1=inp.getCarCost().getCost(pickUpRoute1.getId()-1, pickUpRoute2.getId()-1);
		boolean waintingTime=checkWaitingTime(pickUpRoute1,distpickUpRoute2pickUpRoute1,pickUpRoute2);
		if(pickUpRoute2.getDepartureTime()+distpickUpRoute2pickUpRoute1<=pickUpRoute1.getArrivalTime() && waintingTime) { // checking time windows 
			double distpickUpRoute1dropOffRoute2= inp.getCarCost().getCost(pickUpRoute1.getId()-1, dropOffRoute2.getId()-1);
			waintingTime=checkWaitingTime(dropOffRoute2,distpickUpRoute1dropOffRoute2,pickUpRoute1);
			if(pickUpRoute1.getDepartureTime()+distpickUpRoute1dropOffRoute2<=dropOffRoute2.getArrivalTime() && waintingTime) {	// checking time window
				String key=pickUpRoute2.getSubJobKey()+dropOffRoute2.getSubJobKey();
				Edge connection=ordering.get(1).getDirectoryConnections().get(key);
				if((distpickUpRoute2pickUpRoute1+distpickUpRoute1dropOffRoute2)<=connection.getDetour()) { // checking detour

					listSubJobs.add(pickUpRoute2);
					listSubJobs.add(pickUpRoute1);
					listSubJobs.add(dropOffRoute2);		
					if(vehicleCapacityPart(listSubJobs)) {
						merging=true;
						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections(earlyPart, latePart);
						newRoute.getPartsRoute().add(newPart);}
					//					newRoute.getSubJobsList().add(pickUpRoute2);
					//					newRoute.getSubJobsList().add(pickUpRoute1);
					//					newRoute.getSubJobsList().add(dropOffRoute2);					
				}
			}
		}


		return merging;
	}

	private ArrayList<Parts> orderingParts(Parts earlyPart, Parts latePart) { // pequeno- grande
		ArrayList<Parts> ordering=new ArrayList<>();
		if(earlyPart.getListSubJobs().size()<latePart.getListSubJobs().size()) {
			ordering.add(0,earlyPart);
			ordering.add(latePart);
		}
		else {
			ordering.add(0,latePart);
			ordering.add(earlyPart);
		}
		return ordering;
	}

	private boolean fourSubJobs(Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging=false;
		//
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		// structure de una parte: (pickUp)---(dropOff)
		SubJobs pickUpEarly=earlyPart.getListSubJobs().get(0);
		SubJobs dropOffEarly=earlyPart.getListSubJobs().get(1);
		SubJobs pickUpLate=latePart.getListSubJobs().get(0);
		SubJobs dropOffLate=latePart.getListSubJobs().get(1);

		// Option 1: (pickUpPatientEarly)---(pickUpPatientLate)---(dropOffPatientEarly)---(dropOffPatientLate)
		double pickUpEarlypickUpLate=inp.getCarCost().getCost(pickUpEarly.getId()-1, pickUpLate.getId()-1);
		double dropOffEarlydropOffLate=inp.getCarCost().getCost(dropOffEarly.getId()-1,dropOffLate.getId()-1);
		// connection
		boolean waintingTime=checkWaitingTime(pickUpLate,pickUpEarlypickUpLate,pickUpEarly);
		if(pickUpEarly.getDepartureTime()+pickUpEarlypickUpLate<=pickUpLate.getArrivalTime() && waintingTime) {// Travel time first combination
			waintingTime=checkWaitingTime(dropOffLate,dropOffEarlydropOffLate,dropOffEarly);
			if(dropOffEarly.getDepartureTime()+dropOffEarlydropOffLate<=dropOffLate.getArrivalTime()) { // Travel time second combination
				// control detour early job (pickUpPatientEarly)---x---(dropOffPatientEarly)
				String key=pickUpEarly.getSubJobKey()+dropOffEarly.getSubJobKey();
				Edge connectionEarly=earlyPart.getDirectoryConnections().get(key);
				if(connectionEarly==null) {
					//System.out.println("stop");
				}
				double detourEarly=pickUpEarlypickUpLate+inp.getCarCost().getCost(pickUpLate.getId()-1, dropOffEarly.getId()-1);

				// control detour early job (pickUpPatientLate)---x---(dropOffPatientLate)
				key=pickUpLate.getSubJobKey()+dropOffLate.getSubJobKey();
				Edge connectionLate=latePart.getDirectoryConnections().get(key);
				if(connectionLate==null) {
					//System.out.println("stop");
				}
				double detourLate=dropOffEarlydropOffLate+inp.getCarCost().getCost(pickUpLate.getId()-1, dropOffEarly.getId()-1);
				if(detourEarly<=connectionEarly.getDetour() && detourLate<=connectionLate.getDetour()) {// tour early and late

					listSubJobs.add(pickUpEarly);
					listSubJobs.add(pickUpLate);
					listSubJobs.add(dropOffEarly);
					listSubJobs.add(dropOffLate);
					if(vehicleCapacityPart(listSubJobs)) {
						merging=true;

						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections(earlyPart, latePart);
						newRoute.getPartsRoute().add(newPart);}
					//					newRoute.getSubJobsList().add(pickUpEarly);
					//					newRoute.getSubJobsList().add(pickUpLate);
					//					newRoute.getSubJobsList().add(dropOffEarly);
					//					newRoute.getSubJobsList().add(dropOffLate);
				}
			}	
		}



		// Option 2: (pickUpPatientEarly)---(pickUpPatientLate)---(dropOffPatientEarly)---(dropOffPatientLate)
		if(!merging) {
			double dropOffLatedropOffEarly=inp.getCarCost().getCost(dropOffLate.getId()-1, dropOffEarly.getId()-1);
			waintingTime=checkWaitingTime(pickUpLate,pickUpEarlypickUpLate,pickUpEarly);
			if(pickUpEarly.getDepartureTime()+pickUpEarlypickUpLate<=pickUpLate.getArrivalTime() && waintingTime) { // time window (pickUpPatientEarly)---(pickUpPatientLate)
				waintingTime=checkWaitingTime(dropOffEarly,dropOffLatedropOffEarly,dropOffLate);
				if(dropOffLate.getDepartureTime()+dropOffLatedropOffEarly<=dropOffEarly.getArrivalTime() && waintingTime) { // time window (dropOffPatientEarly)---(dropOffPatientLate)
					// distance detour
					String key=pickUpLate.getSubJobKey()+dropOffLate.getSubJobKey();
					Edge edgeConnectionLate=latePart.getDirectoryConnections().get(key);
					double distDetour=pickUpEarlypickUpLate+edgeConnectionLate.getTime()+dropOffLatedropOffEarly;
					key=pickUpLate.getSubJobKey()+dropOffLate.getSubJobKey();
					Edge directConnection= earlyPart.getDirectoryConnections().get(key);
					if(distDetour<=directConnection.getDetour()) { // detour
						merging=true;
						listSubJobs.add(pickUpEarly);
						listSubJobs.add(pickUpLate);
						listSubJobs.add(dropOffLate);
						listSubJobs.add(dropOffEarly);
						if(vehicleCapacityPart(listSubJobs)) {
							merging=true;
							newPart.setListSubJobs(listSubJobs, inp, test);
							newRoute.getPartsRoute().add(newPart);
							newPart.settingConnections(earlyPart, latePart);
							newRoute.getPartsRoute().add(newPart);}
						//						newRoute.getSubJobsList().add(pickUpEarly);
						//						newRoute.getSubJobsList().add(pickUpLate);
						//						newRoute.getSubJobsList().add(dropOffLate);
						//						newRoute.getSubJobsList().add(dropOffEarly);
					}
				}
			}
		}
		return merging;
	}

	private boolean seatingHeading(Route earlyRoute, Route lateRoute, Route newRoute) {
		boolean headingMerge= false;// la early es la ruta de referencia  - eso quiere decir que al final el array mergingPart debe ser igual a la ruta más cercana
		// selecting depot
		//SubJobs depot=new SubJobs(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0));
		//newRoute.getSubJobsList().add(depot);
		newRoute.getPartsRoute().add(earlyRoute.getPartsRoute().get(0));
		headingMerge=mergingHeading(earlyRoute,lateRoute,newRoute);
		if(headingMerge) { // setting the new amount of people
			/*Actualizando la nueva ruta*/
			newRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople()+lateRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			//newRoute.getSubJobsList().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople()+lateRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			newRoute.setAmountParamedic(earlyRoute.getAmountParamedic()+lateRoute.getAmountParamedic());
			newRoute.setHomeCareStaff(earlyRoute.getHomeCareStaff()+lateRoute.getHomeCareStaff());
			/*Actualizando la ruta que queda*/
			lateRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(0);
		}
		else {
			//newRoute.getSubJobsList().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			newRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			newRoute.setAmountParamedic(earlyRoute.getAmountParamedic());
			newRoute.setHomeCareStaff(earlyRoute.getHomeCareStaff());
			/*Actualizando la ruta que queda*/

			Parts p=earlyRoute.getPartsRoute().get(1);
			newRoute.getPartsRoute().add(p);

		}

		return headingMerge;
	}

	//	private boolean mergingHeading(Route earlyRoute, Route lateRoute, Route newRoute) {
	//		// este método lo que hace es modificar la ruta temprana la earlyRoute
	//		boolean feasibleOption1=false;
	//
	//		mergingPart(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),newRoute,feasibleOption1);
	//
	//		return feasibleOption1;
	//	}


	private boolean mergingHeading(Route earlyRoute, Route lateRoute, Route newRoute) {
		// este método lo que hace es modificar la ruta temprana la earlyRoute
		boolean feasibleOption1=false;

		//newRoute.getSubJobsList().add(earlyRoute.getSubJobsList().get(0).getSubJobs().get(0));
		// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare 
		int options=typeOfParts(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1)); 

		switch(options) {
		case 1 :// 1 patient-patient 
			feasibleOption1=mergingPatientPatient(options,earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1), newRoute);// option1
			break;

		case 2 : // 2 homeCare-patient or patient-homeCare
			feasibleOption1=mergingPatientClient(options,earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1), earlyRoute,newRoute);// option1

			break; // optional

		default : // 3 homeCare-homeCare
			feasibleOption1=mergingClients(options,earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1), newRoute);// option1
		}
		return feasibleOption1;
	}
	private boolean mergingClients(int options, Parts early, Parts late, Route newRoute) {
		boolean feasible=keepTimeClients(options,early,late,newRoute);
		if(!feasible) {
			feasible=keepTimeClientsInfeasible(options,early,late,newRoute);}
		return feasible;
	}


	private boolean keepTimeClientsInfeasible(int options, Parts early, Parts late, Route newRoute) {
		boolean feasible=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		SubJobs earlyDropOff=early.getListSubJobs().get(0);
		SubJobs lateDropOff=late.getListSubJobs().get(0);
		Edge removedConncetion= new Edge(depot,lateDropOff,inp,test);
		Edge conncetionDepot= new Edge(depot,earlyDropOff,inp,test);
		/*current distance*/
		double distEarlyDropOffLateDropOff=inp.getCarCost().getCost(earlyDropOff.getId()-1, lateDropOff.getId()-1);
		// boolean waintingTime=checkWaitingTime(lateDropOff,conncetionDepot.getTime()+distEarlyDropOffLateDropOff,earlyDropOff);
		if(earlyDropOff.getDepartureTime()<=lateDropOff.getArrivalTime() ) { // time window
			//	if((conncetionDepot.getTime()+distEarlyDropOffLateDropOff)<=removedConncetion.getDetour()) {// checking detour

			listSubJobs.add(earlyDropOff);
			listSubJobs.add(lateDropOff);
			if(vehicleCapacityPart(listSubJobs)) {
				feasible=true;
				newPart.setListSubJobs(listSubJobs, inp, test);
				newPart.settingConnections(early,late);
				newRoute.getPartsRoute().add(newPart);
			}

			//				newRoute.getSubJobsList().add(earlyDropOff);
			//				newRoute.getSubJobsList().add(lateDropOff);
			//}
		}

		return feasible;
	}




	private boolean keepTimeClients(int options, Parts early, Parts late, Route newRoute) {
		boolean feasible=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		SubJobs earlyDropOff=early.getListSubJobs().get(0);
		SubJobs lateDropOff=late.getListSubJobs().get(0);
		Edge removedConncetion= new Edge(depot,lateDropOff,inp,test);
		Edge conncetionDepot= new Edge(depot,earlyDropOff,inp,test);
		/*current distance*/
		double distEarlyDropOffLateDropOff=inp.getCarCost().getCost(earlyDropOff.getId()-1, lateDropOff.getId()-1);
		boolean waintingTime=checkWaitingTime(lateDropOff,conncetionDepot.getTime()+distEarlyDropOffLateDropOff,earlyDropOff);
		if((earlyDropOff.getDepartureTime()+conncetionDepot.getTime()+distEarlyDropOffLateDropOff)<=lateDropOff.getArrivalTime() && waintingTime) { // time window
			if((conncetionDepot.getTime()+distEarlyDropOffLateDropOff)<=removedConncetion.getDetour()) {// checking detour

				listSubJobs.add(earlyDropOff);
				listSubJobs.add(lateDropOff);
				if(vehicleCapacityPart(listSubJobs)) {
					feasible=true;
					newPart.setListSubJobs(listSubJobs, inp, test);
					newPart.settingConnections(early,late);
					newRoute.getPartsRoute().add(newPart);
				}

				//				newRoute.getSubJobsList().add(earlyDropOff);
				//				newRoute.getSubJobsList().add(lateDropOff);
			}
		}

		return feasible;
	}

	private boolean mergingPatientClient(int options, Parts early, Parts late, Route earlyRoute, Route newRoute) {
		boolean merging=keepTimePatientClient(options,early,late,newRoute);
		if(!merging) {
			merging=keepTimePatientClientInfeasible(options,early,late,newRoute);}
		return merging;
	}


	private boolean keepTimePatientClientInfeasible(int options, Parts early, Parts late, Route newRoute) {
		boolean merging=false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// calling subjobs
		SubJobs pickUpEarly=null;
		SubJobs dropoffEarly=null;
		SubJobs pickUpLate=null;
		SubJobs dropoffLate=null;
		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opción en que el prime trabajo sea uno asociado a un home care staff 
			if( early.getListSubJobs().get(0).getId()!=1) { 
				// early <- home care staff  late<- patient
				dropoffEarly=early.getListSubJobs().get(0); // home care
				pickUpLate=late.getListSubJobs().get(0); // patient
				dropoffLate=late.getListSubJobs().get(1); 

				//boolean feasibleTW=ckeckTimeWindow(pickUpLate,dropoffEarly,dropoffLate);
				//boolean feasibleDetour=checkDetour(depot,pickUpLate,dropoffEarly,dropoffLate,early);
				//if(feasibleTW && feasibleDetour ){
				if(pickUpLate.getDepartureTime()<dropoffLate.getArrivalTime()) { //(Pick patient)---(dropoff home care staff)
					if(dropoffLate.getDepartureTime()<dropoffEarly.getArrivalTime()) {
						listSubJobs.add(pickUpLate);
						listSubJobs.add(dropoffEarly);
						listSubJobs.add(dropoffLate);
						if(vehicleCapacityPart(listSubJobs)) {
							merging=true;
							newPart.setListSubJobs(listSubJobs, inp, test);
							newPart.settingConnections(early,late);
							newRoute.getPartsRoute().add(newPart);
						}}}

				//				newRoute.getSubJobsList().add(pickUpEarly);
				//				newRoute.getSubJobsList().add(dropoffLate);
				//				newRoute.getSubJobsList().add(dropoffEarly);
				//}
			}
		}
		else {// opción en que el primer trabajo se uno asociado a  un paciente
			// early <- patient  late<- home care staff
			if(early.getListSubJobs().size()==2 && late.getListSubJobs().size()==1) { 
				if(late.getListSubJobs().get(0).getId()!=1) {
					pickUpEarly=early.getListSubJobs().get(0);  // patient
					dropoffEarly=early.getListSubJobs().get(1);
					dropoffLate=late.getListSubJobs().get(0); // home care staff
					Edge connectionToRemove= new Edge(depot,dropoffLate,inp,test);
					Edge connectionDepot= new Edge(depot,pickUpEarly,inp,test);
					double distDropOffEarlyPickUpLate=inp.getCarCost().getCost(dropoffEarly.getId()-1, dropoffLate.getId());
					double distDropOffEarlyDropOffLate=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId());
					boolean waintingTime=checkWaitingTime(dropoffLate,distDropOffEarlyPickUpLate,pickUpEarly);
					if(pickUpEarly.getDepartureTime()+distDropOffEarlyPickUpLate<dropoffLate.getArrivalTime() && waintingTime) { // time window (depot)---x---(pick up patient)
						waintingTime=checkWaitingTime(dropoffEarly,distDropOffEarlyDropOffLate,dropoffLate);
						if(dropoffLate.getDepartureTime()+distDropOffEarlyDropOffLate<=dropoffEarly.getArrivalTime() && waintingTime) {
							if(connectionDepot.getTime()+distDropOffEarlyPickUpLate<=connectionToRemove.getDetour()) { // detour 


								listSubJobs.add(dropoffEarly);
								listSubJobs.add(pickUpLate);
								listSubJobs.add(dropoffLate);
								if(vehicleCapacityPart(listSubJobs)) {
									merging=true;
									newPart.setListSubJobs(listSubJobs, inp, test);
									newPart.settingConnections(early,late);
									newRoute.getPartsRoute().add(newPart);}
							}
						}
					}
				}
			}
		}
		return merging;
	}

	private boolean changeTimePatientClient(int options, Parts early, Parts late, Route earlyRoute, Route newRoute) {
		boolean merging=false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// calling subjobs
		SubJobs pickUpEarly=null;
		SubJobs dropoffEarly=null;
		SubJobs pickUpLate=null;
		SubJobs dropoffLate=null;

		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opción en que el prime trabajo sea uno asociado a un home care staff 
			if( early.getListSubJobs().get(0).getId()!=1) { 
				// patient <- home care staff  late<- patient
				dropoffEarly=early.getListSubJobs().get(0); // home care
				pickUpLate=late.getListSubJobs().get(0); // patient
				dropoffLate=late.getListSubJobs().get(1);  // patient

				boolean feasibleTW=changeTimeWindow(pickUpEarly,dropoffLate,dropoffEarly);
				boolean feasibleDetour=checkDetour(depot,pickUpEarly,dropoffLate,dropoffEarly,early);
				boolean feasibleNextPart=true;
				if(earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()!=1) {
					double distancedropoffLateNextPart=inp.getCarCost().getCost(dropoffLate.getId()-1, earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()-1);
					if(dropoffLate.getDepartureTime()+distancedropoffLateNextPart<=earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getArrivalTime()) {
						feasibleNextPart=true;
					}
					else {
						feasibleNextPart=false;
					}
				}

				if(feasibleTW && feasibleDetour && feasibleNextPart){
					merging=true;
					listSubJobs.add(pickUpLate);
					listSubJobs.add(dropoffEarly);
					listSubJobs.add(dropoffLate);
					newPart.setListSubJobs(listSubJobs, inp, test);
					newPart.settingConnections(early,late);
					newRoute.getPartsRoute().add(newPart);
				}
			}
		}
		else {// opción en que el primer trabajo se uno asociado a  un paciente
			// early <- patient  late<- home care staff

			if(late.getListSubJobs().get(0).getId()!=1) {
				pickUpEarly=early.getListSubJobs().get(0);  // patient
				dropoffEarly=early.getListSubJobs().get(1); // patient
				dropoffLate=late.getListSubJobs().get(0); // home care staff
				Edge connectionToRemove= new Edge(depot,dropoffLate,inp,test);
				Edge connectionDepot= new Edge(depot,pickUpEarly,inp,test);
				double distDropOffEarlyPickUpLate=inp.getCarCost().getCost(dropoffEarly.getId()-1, dropoffLate.getId());


				double loadUnloadTime=0;
				double registration=0;
				if(dropoffLate.isClient()) {
					loadUnloadTime=test.getloadTimeHomeCareStaff();
				}
				else {
					loadUnloadTime=test.getloadTimePatient();
					registration=test.getRegistrationTime();
				}
				double departure=dropoffEarly.getDepartureTime()+distDropOffEarlyPickUpLate;
				double arrival=departure-loadUnloadTime;
				double startServiceTime=arrival+registration;
				boolean feasibleNextPart=true;
				if(earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()!=1) {
					double distancedropoffLateNextPart=inp.getCarCost().getCost(dropoffLate.getId()-1, earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()-1);
					if(dropoffLate.getDepartureTime()+distancedropoffLateNextPart<=earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getArrivalTime()) {
						feasibleNextPart=true;
					}
					else {
						feasibleNextPart=false;
					}
				}
				if(startServiceTime>=dropoffLate.getStartTime() && startServiceTime<=dropoffLate.getEndTime() && feasibleNextPart) {
					if(connectionDepot.getTime()+distDropOffEarlyPickUpLate<=connectionToRemove.getDetour()) { // detour 
						merging=true;
						listSubJobs.add(pickUpEarly);
						listSubJobs.add(dropoffEarly);
						listSubJobs.add(dropoffLate);
						newPart.setListSubJobs(listSubJobs, inp, test);
						newPart.settingConnections(early,late);
						newRoute.getPartsRoute().add(newPart);
					}
				}		
			}
		}
		return merging;
	}


	private boolean keepTimePatientClient(int options, Parts early, Parts late, Route newRoute) {
		boolean merging=false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// calling subjobs
		SubJobs pickUpEarly=null;
		SubJobs dropoffEarly=null;
		SubJobs pickUpLate=null;
		SubJobs dropoffLate=null;

		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opción en que el prime trabajo sea uno asociado a un home care staff 
			if( early.getListSubJobs().get(0).getId()!=1) { 
				// early <- home care staff  late<- patient
				dropoffEarly=early.getListSubJobs().get(0); // home care
				pickUpLate=late.getListSubJobs().get(0); // patient
				dropoffLate=late.getListSubJobs().get(1); 

				boolean feasibleTW=ckeckTimeWindow(pickUpLate,dropoffEarly,dropoffLate);
				boolean feasibleDetour=checkDetour(depot,pickUpLate,dropoffEarly,dropoffLate,early);
				if(feasibleTW && feasibleDetour ){

					listSubJobs.add(pickUpLate);
					listSubJobs.add(dropoffEarly);
					listSubJobs.add(dropoffLate);
					if(vehicleCapacityPart(listSubJobs)) {
						merging=true;
						newPart.setListSubJobs(listSubJobs, inp, test);
						newPart.settingConnections(early,late);
						newRoute.getPartsRoute().add(newPart);
					}

					//				newRoute.getSubJobsList().add(pickUpEarly);
					//				newRoute.getSubJobsList().add(dropoffLate);
					//				newRoute.getSubJobsList().add(dropoffEarly);
				}
			}
		}
		else {// opción en que el primer trabajo se uno asociado a  un paciente
			// early <- patient  late<- home care staff
			if(early.getListSubJobs().size()==2 && late.getListSubJobs().size()==1) { 
				if(late.getListSubJobs().get(0).getId()!=1) {
					pickUpEarly=early.getListSubJobs().get(0);  // patient
					dropoffEarly=early.getListSubJobs().get(1);
					dropoffLate=late.getListSubJobs().get(0); // home care staff
					Edge connectionToRemove= new Edge(depot,dropoffLate,inp,test);
					Edge connectionDepot= new Edge(depot,pickUpEarly,inp,test);
					double distDropOffEarlyPickUpLate=inp.getCarCost().getCost(dropoffEarly.getId()-1, dropoffLate.getId());
					double distDropOffEarlyDropOffLate=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId());
					boolean waintingTime=checkWaitingTime(dropoffLate,distDropOffEarlyPickUpLate,pickUpEarly);
					if(pickUpEarly.getDepartureTime()+distDropOffEarlyPickUpLate<dropoffLate.getArrivalTime() && waintingTime) { // time window (depot)---x---(pick up patient)
						waintingTime=checkWaitingTime(dropoffEarly,distDropOffEarlyDropOffLate,dropoffLate);
						if(dropoffLate.getDepartureTime()+distDropOffEarlyDropOffLate<=dropoffEarly.getArrivalTime() && waintingTime) {
							if(connectionDepot.getTime()+distDropOffEarlyPickUpLate<=connectionToRemove.getDetour()) { // detour 


								listSubJobs.add(dropoffEarly);
								listSubJobs.add(pickUpLate);
								listSubJobs.add(dropoffLate);
								if(vehicleCapacityPart(listSubJobs)) {
									merging=true;
									newPart.setListSubJobs(listSubJobs, inp, test);
									newPart.settingConnections(early,late);
									newRoute.getPartsRoute().add(newPart);}
							}
						}
					}
				}
			}
		}
		return merging;
	}

	private boolean checkDetour(SubJobs depot, SubJobs pickUpEarly, SubJobs dropoffLate, SubJobs dropoffEarly, Parts early) {
		boolean feasible=false;
		double tvPickUpEarlyDropOffLate=inp.getCarCost().getCost(pickUpEarly.getId()-1, dropoffLate.getId()-1);
		double tvDropOffLateDropOffEarly=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId()-1);
		/* current connection to depot*/
		Edge depotConnection=new Edge(depot, pickUpEarly,inp,test);


		/*removed connection to depot*/
		Edge depotConnectionRemoved=new Edge(depot, dropoffLate,inp,test);
		if(depotConnection.getTime()+tvPickUpEarlyDropOffLate<=depotConnectionRemoved.getDetour()) { //detour: (Depot)---x---(dropoff home care staff)

			String key=pickUpEarly.getSubJobKey()+dropoffEarly.getSubJobKey();
			Edge pickUpDropOff=early.getDirectoryConnections().get(key);
			if(tvPickUpEarlyDropOffLate+tvDropOffLateDropOffEarly<=pickUpDropOff.getDetour()) { //detour: (Pick patient)---x---(dropoff patient)
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean changeTimeWindow(SubJobs pickUpEarly, SubJobs dropoffLate, SubJobs dropoffEarly) { //(Pick patient)---(dropoff home care staff)---(dropoff patient)
		boolean feasible=false;
		double tvDropOffLateDropOffEarly=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId()-1);
		double loadUnloadTime=0;
		double registration=0;
		if(dropoffLate.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=dropoffEarly.getDepartureTime()-tvDropOffLateDropOffEarly;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;

		if(pickUpEarly.getDepartureTime()<=arrival  && startServiceTime>=dropoffLate.getStartTime() && startServiceTime<=dropoffLate.getEndTime()) {
			dropoffLate.setarrivalTime(arrival);
			dropoffLate.setStartServiceTime(startServiceTime);
			dropoffLate.setEndServiceTime(startServiceTime+dropoffLate.getReqTime());
			dropoffLate.setdepartureTime(departure);
			feasible=true;
		}
		return feasible;
	}

	private boolean ckeckTimeWindow(SubJobs pickUpEarly, SubJobs dropoffLate, SubJobs dropoffEarly) { //(Pick patient)---(dropoff home care staff)---(dropoff patient)
		boolean feasible=false;
		double tvPickUpEarlyDropOffLate=inp.getCarCost().getCost(pickUpEarly.getId()-1, dropoffLate.getId()-1);
		double tvDropOffLateDropOffEarly=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId()-1);
		if(pickUpEarly.getDepartureTime()+tvPickUpEarlyDropOffLate<dropoffLate.getArrivalTime()) { //(Pick patient)---(dropoff home care staff)
			if(dropoffLate.getDepartureTime()+tvDropOffLateDropOffEarly<dropoffEarly.getArrivalTime()) {//(dropoff home care staff)---(dropoff patient)
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean mergingPatientPatient(int typeParts, Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging= keepTime(typeParts,earlyPart,latePart,newRoute);
		if(!merging) {
			merging= keepTimeInfesible(typeParts,earlyPart,latePart,newRoute);
		}
		return merging;
	}





	private boolean keepTimeInfesible(int typeParts, Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging= false;
		if(earlyPart.getListSubJobs().size()==latePart.getListSubJobs().size() && earlyPart.getListSubJobs().size()==2) {
			ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
			Parts newPart=new Parts();
			// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare

			//SubJobs depot=newRoute.getSubJobsList().get(0);
			SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
			// option 1: depot - patient Early pick up - Patient Late pick up - patient Early drop-off - Patient Late drop-off
			SubJobs pickUpPatientEarly=earlyPart.getListSubJobs().get(0);
			SubJobs dropOffPatientEarly=earlyPart.getListSubJobs().get(1);
			SubJobs pickUpPatientLate=latePart.getListSubJobs().get(0);
			SubJobs dropOffPatientLate=latePart.getListSubJobs().get(1);
			Edge connectionToDepot=new Edge(depot,pickUpPatientEarly,inp,test);
			Edge removeConnection=new Edge(depot,pickUpPatientLate,inp,test);

			/*Distance Option*/
			double travelTime12=inp.getCarCost().getCost(pickUpPatientEarly.getId()-1, pickUpPatientLate.getId()-1);
			double travelTime23=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
			double travelTime34=inp.getCarCost().getCost(dropOffPatientEarly.getId()-1, dropOffPatientLate.getId()-1);

			/* detour early -early */	
			String key= pickUpPatientEarly.getSubJobKey()+dropOffPatientEarly.getSubJobKey();// to check detour ->depot- tarde
			Edge PatientHouseMedicalCentreEarly= earlyPart.getDirectoryConnections().get(key);
			double disHomeMedicalCentreEarly=travelTime12+travelTime34;

			double depotJobLate=connectionToDepot.getTime()+travelTime12;

			// Early <- detour temprano - temprano
			double distHomeMedicalCentreEarlyOption2=travelTime12+travelTime23;	

			/* detour tarde - tarde */
			key= pickUpPatientLate.getSubJobKey()+dropOffPatientLate.getSubJobKey();// to check detour ->depot- tarde
			Edge PatientHouseMedicalCentreLate= latePart.getDirectoryConnections().get(key);
			double distHomeMedicalCentrelteOption2Late=travelTime23+travelTime34;	

			/*Option 1<-pick up temprano - pick up tarde - drop off temprano - drop off tarde*/
			//boolean waintingTime=checkWaitingTime(pickUpPatientLate,travelTime12,pickUpPatientEarly);
			//if((pickUpPatientEarly.getDepartureTime()+travelTime12)<=pickUpPatientLate.getArrivalTime() && waintingTime) {// checking time windows (Early pick up)--- (patient Late pick up)
			//waintingTime=checkWaitingTime(dropOffPatientEarly,travelTime23,pickUpPatientLate);
			//if((pickUpPatientLate.getDepartureTime()+travelTime23)<=dropOffPatientEarly.getArrivalTime() && waintingTime) {// checking time windows (patient Late pick up)---(patient Early drop-off)
			//	waintingTime=checkWaitingTime(dropOffPatientLate,travelTime34,dropOffPatientEarly);
			if((dropOffPatientEarly.getDepartureTime())<=dropOffPatientLate.getArrivalTime()) {// checking time windows (patient Late pick up)---(patient Early drop-off)
				// detour current distance (shared trips) vs distance (individual trips)
				boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);// connección con el depot y connección entre trabajos
				boolean detourEarlySubjob=checkingDetourEarlyPatient(disHomeMedicalCentreEarly,PatientHouseMedicalCentreEarly);// connección con el depot y connección entre trabajos
				if(detourLateSubjob && detourEarlySubjob) {

					listSubJobs.add(pickUpPatientEarly);
					listSubJobs.add(pickUpPatientLate);
					listSubJobs.add(dropOffPatientEarly);
					listSubJobs.add(dropOffPatientLate);
					if(vehicleCapacityPart(listSubJobs)) {
						merging= true;
						newPart.setListSubJobs(listSubJobs, inp, test);
						newPart.settingConnections(earlyPart,latePart);
						newRoute.getPartsRoute().add(newPart);
					}

					//						newRoute.getSubJobsList().add(pickUpPatientEarly);
					//						newRoute.getSubJobsList().add(pickUpPatientLate);
					//						newRoute.getSubJobsList().add(dropOffPatientEarly);
					//						newRoute.getSubJobsList().add(dropOffPatientLate);
				}
			}
			//}
			//}

			/*Option 2 <-pick up temprano - pick up tarde - drop off tarde - drop off temprano*/
			if(!merging) {
				/* Distance option 2*/ 

				double traveltimePickupDropOff=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientLate.getId()-1);
				double traveltimeDropOffDropOff=inp.getCarCost().getCost(dropOffPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
				double distPickUpDropOffLate=travelTime12+traveltimePickupDropOff+traveltimeDropOffDropOff;
				// time windows
				if(dropOffPatientLate.getDepartureTime()<=dropOffPatientEarly.getArrivalTime()) {// checking time windows (Early pick up)--- (patient Late pick up)
					// checking the detour
					//boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);
					//boolean detourEarlySubJob=checkingDetourEarly(distPickUpDropOffLate,PatientHouseMedicalCentreEarly);
					//if(detourLateSubjob && detourEarlySubJob) {

					listSubJobs.add(pickUpPatientEarly);
					listSubJobs.add(pickUpPatientLate);
					listSubJobs.add(dropOffPatientLate);
					listSubJobs.add(dropOffPatientEarly);
					if(vehicleCapacityPart(listSubJobs)) {
						newPart.setListSubJobs(listSubJobs, inp, test);
						newPart.settingConnections(earlyPart,latePart);
						newRoute.getPartsRoute().add(newPart);
						merging= true;
					}

					//}

				}
			}
		}
		return merging;
	}

	private boolean keepTime(int typeParts, Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging= false;
		if(earlyPart.getListSubJobs().size()==latePart.getListSubJobs().size() && earlyPart.getListSubJobs().size()==2) {
			ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
			Parts newPart=new Parts();
			// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare

			//SubJobs depot=newRoute.getSubJobsList().get(0);
			SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
			// option 1: depot - patient Early pick up - Patient Late pick up - patient Early drop-off - Patient Late drop-off
			SubJobs pickUpPatientEarly=earlyPart.getListSubJobs().get(0);
			SubJobs dropOffPatientEarly=earlyPart.getListSubJobs().get(1);
			SubJobs pickUpPatientLate=latePart.getListSubJobs().get(0);
			SubJobs dropOffPatientLate=latePart.getListSubJobs().get(1);
			Edge connectionToDepot=new Edge(depot,pickUpPatientEarly,inp,test);
			Edge removeConnection=new Edge(depot,pickUpPatientLate,inp,test);

			/*Distance Option*/
			double travelTime12=inp.getCarCost().getCost(pickUpPatientEarly.getId()-1, pickUpPatientLate.getId()-1);
			double travelTime23=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
			double travelTime34=inp.getCarCost().getCost(dropOffPatientEarly.getId()-1, dropOffPatientLate.getId()-1);

			/* detour early -early */	
			String key= pickUpPatientEarly.getSubJobKey()+dropOffPatientEarly.getSubJobKey();// to check detour ->depot- tarde
			Edge PatientHouseMedicalCentreEarly= earlyPart.getDirectoryConnections().get(key);
			double disHomeMedicalCentreEarly=travelTime12+travelTime34;

			double depotJobLate=connectionToDepot.getTime()+travelTime12;

			// Early <- detour temprano - temprano
			double distHomeMedicalCentreEarlyOption2=travelTime12+travelTime23;	

			/* detour tarde - tarde */
			key= pickUpPatientLate.getSubJobKey()+dropOffPatientLate.getSubJobKey();// to check detour ->depot- tarde
			Edge PatientHouseMedicalCentreLate= latePart.getDirectoryConnections().get(key);
			double distHomeMedicalCentrelteOption2Late=travelTime23+travelTime34;	

			/*Option 1<-pick up temprano - pick up tarde - drop off temprano - drop off tarde*/
			boolean waintingTime=checkWaitingTime(pickUpPatientLate,travelTime12,pickUpPatientEarly);
			if((pickUpPatientEarly.getDepartureTime()+travelTime12)<=pickUpPatientLate.getArrivalTime() && waintingTime) {// checking time windows (Early pick up)--- (patient Late pick up)
				waintingTime=checkWaitingTime(dropOffPatientEarly,travelTime23,pickUpPatientLate);
				if((pickUpPatientLate.getDepartureTime()+travelTime23)<=dropOffPatientEarly.getArrivalTime() && waintingTime) {// checking time windows (patient Late pick up)---(patient Early drop-off)
					waintingTime=checkWaitingTime(dropOffPatientLate,travelTime34,dropOffPatientEarly);
					if((dropOffPatientEarly.getDepartureTime()+travelTime34)<=dropOffPatientLate.getArrivalTime() && waintingTime) {// checking time windows (patient Late pick up)---(patient Early drop-off)
						// detour current distance (shared trips) vs distance (individual trips)
						boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);// connección con el depot y connección entre trabajos
						boolean detourEarlySubjob=checkingDetourEarlyPatient(disHomeMedicalCentreEarly,PatientHouseMedicalCentreEarly);// connección con el depot y connección entre trabajos
						if(detourLateSubjob && detourEarlySubjob) {

							listSubJobs.add(pickUpPatientEarly);
							listSubJobs.add(pickUpPatientLate);
							listSubJobs.add(dropOffPatientEarly);
							listSubJobs.add(dropOffPatientLate);
							if(vehicleCapacityPart(listSubJobs)) {
								merging= true;
								newPart.setListSubJobs(listSubJobs, inp, test);
								newPart.settingConnections(earlyPart,latePart);
								newRoute.getPartsRoute().add(newPart);
							}

							//						newRoute.getSubJobsList().add(pickUpPatientEarly);
							//						newRoute.getSubJobsList().add(pickUpPatientLate);
							//						newRoute.getSubJobsList().add(dropOffPatientEarly);
							//						newRoute.getSubJobsList().add(dropOffPatientLate);
						}
					}
				}
			}

			/*Option 2 <-pick up temprano - pick up tarde - drop off tarde - drop off temprano*/
			if(!merging) {
				/* Distance option 2*/ 

				double traveltimePickupDropOff=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientLate.getId()-1);
				double traveltimeDropOffDropOff=inp.getCarCost().getCost(dropOffPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
				double distPickUpDropOffLate=travelTime12+traveltimePickupDropOff+traveltimeDropOffDropOff;
				// time windows
				if((dropOffPatientLate.getDepartureTime()+traveltimeDropOffDropOff)<=dropOffPatientEarly.getArrivalTime()) {// checking time windows (Early pick up)--- (patient Late pick up)
					// checking the detour
					boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);
					boolean detourEarlySubJob=checkingDetourEarly(distPickUpDropOffLate,PatientHouseMedicalCentreEarly);
					if(detourLateSubjob && detourEarlySubJob) {

						listSubJobs.add(pickUpPatientEarly);
						listSubJobs.add(pickUpPatientLate);
						listSubJobs.add(dropOffPatientLate);
						listSubJobs.add(dropOffPatientEarly);
						if(vehicleCapacityPart(listSubJobs)) {
							newPart.setListSubJobs(listSubJobs, inp, test);
							newPart.settingConnections(earlyPart,latePart);
							newRoute.getPartsRoute().add(newPart);
							merging= true;
						}

					}

				}
			}}
		return merging;
	}

	private boolean checkWaitingTime(SubJobs dropOffPatientLate, double travelTime34, SubJobs dropOffPatientEarly) {
		boolean waitingTime=false;

		// checking the waiting time for the node dropOffPatientEarly
		// structure (dropOffPatientLate)---travel34---(dropOffPatientEarly)
		if(dropOffPatientEarly.getArrivalTime()>=(dropOffPatientLate.getDepartureTime()+travelTime34)) {
			if(dropOffPatientEarly.getArrivalTime()-(dropOffPatientLate.getDepartureTime()+travelTime34)<=test.getCumulativeWaitingTime()) {
				waitingTime=true;	
			}}
		return waitingTime;
	}

	private boolean checkingDetourEarly(double distPickUpDropOffLate, Edge patientHouseMedicalCentreEarly) {
		boolean merging=false;
		if(distPickUpDropOffLate<=patientHouseMedicalCentreEarly.getDetour()){
			merging=true;
		}
		return merging;
	}

	private boolean checkingDetourEarlyPatient(double depotJobLate, Edge removeConnection) {
		boolean detourLate=false;
		if(depotJobLate<=removeConnection.getDetour()) {
			detourLate=true;
		}

		return detourLate;
	}


	private boolean checkingDetourPatient(double detour1, Edge directConnectionRemoved,double detour2, Edge directConnection) {
		boolean detourLate=false;
		if(detour1<=directConnectionRemoved.getDetour() && detour2<=directConnection.getDetour()) {
			detourLate=true;
		}
		return detourLate;
	}

	private boolean mergingIntraParts(Parts earlyPartRoute, Parts latePartRoute) {
		boolean merge= false;
		SubJobs firstJobEarly=earlyPartRoute.getListSubJobs().get(0);
		SubJobs secondJobEarly=earlyPartRoute.getListSubJobs().get(0);

		return merge;
	}

	private boolean evaluationDetour(Route earlyRoute, Route lateRoute) {
		boolean evaluation=false;
		SubJobs depot=earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// Evaluation con el depot
		int options=typeOfParts(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1)) ; 
		switch(options) {// 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare  
		case 1 :// 1 patient-patient 
			evaluation=detourPatients(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),depot);
			break;

		case 2 : // 2 homeCare-patient or patient-homeCare
			evaluation=detourPatientClient(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),depot);
			break; // optional

		default : // 3 homeCare-homeCare
			evaluation=detourClients(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),depot);
		}

		return evaluation;
	}

	private boolean detourPatients(Parts early, Parts late, SubJobs depot) {
		boolean feasible=false;
		// checking respect to early patient
		SubJobs earlyPatientHouse=early.getListSubJobs().get(0);
		SubJobs earlyPatientMedicalCentre=early.getListSubJobs().get(1);
		SubJobs latePatientHouse=late.getListSubJobs().get(0);
		SubJobs latePatientMedicalCentre=late.getListSubJobs().get(1);
		// counting distance

		/*Option 1<-temprano - tarde - tarde - temprano*/
		// early patient
		String key= earlyPatientHouse.getSubJobKey()+earlyPatientMedicalCentre.getSubJobKey();  //to check detour ->temprano- temprano
		Edge HomeMedicalCentreEarly= early.getDirectoryConnections().get(key); 	
		double distHomeMedicalCentreEarlyOption1=inp.getCarCost().getCost(earlyPatientHouse.getId()-1, latePatientHouse.getId()-1)+inp.getCarCost().getCost(latePatientHouse.getId()-1,latePatientMedicalCentre.getId()-1)+inp.getCarCost().getCost(latePatientMedicalCentre.getId()-1,earlyPatientMedicalCentre.getId()-1);

		// late pateint
		Edge depotPatientHouseLate=new Edge(depot,latePatientHouse,inp,test);
		double distDepotHomeMedicalCentrelteOption1Late=inp.getCarCost().getCost(depot.getId()-1, earlyPatientHouse.getId()-1)+inp.getCarCost().getCost(earlyPatientHouse.getId()-1,latePatientHouse.getId()-1);

		if(distHomeMedicalCentreEarlyOption1<HomeMedicalCentreEarly.getDetour() && distDepotHomeMedicalCentrelteOption1Late<depotPatientHouseLate.getDetour()) {
			feasible=true;
		}
		else {
			/*Option 2<-temprano - tarde - temprano - tarde*/
			// Early <- detour temprano - temprano
			double distHomeMedicalCentreEarlyOption2=inp.getCarCost().getCost(earlyPatientHouse.getId()-1, latePatientHouse.getId()-1)+inp.getCarCost().getCost(latePatientHouse.getId()-1,earlyPatientMedicalCentre.getId()-1);	
			// Early <- detour tarde - tarte
			key= latePatientHouse.getSubJobKey()+latePatientMedicalCentre.getSubJobKey();// to check detour ->depot- tarde
			Edge PatientHouseMedicalCentreLate= late.getDirectoryConnections().get(key);
			double distHomeMedicalCentrelteOption2Late=inp.getCarCost().getCost(latePatientHouse.getId()-1, earlyPatientMedicalCentre.getId()-1)+inp.getCarCost().getCost(earlyPatientMedicalCentre.getId()-1,latePatientMedicalCentre.getId()-1);	
			if(distHomeMedicalCentreEarlyOption1<HomeMedicalCentreEarly.getDetour() && distHomeMedicalCentreEarlyOption2<HomeMedicalCentreEarly.getDetour() && distHomeMedicalCentrelteOption2Late<PatientHouseMedicalCentreLate.getDetour()) {
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean detourPatientClient(Parts early, Parts late, SubJobs depot) {
		boolean feasible=false;
		double distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);

		Edge toCheck=new Edge(depot,late.getListSubJobs().get(0),inp,test);

		if(early.getListSubJobs().size()==1) { // el primer trabajo de la ruta es un cliente
			distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);
			if(toCheck.getDetour()>distanceAccum) {
				feasible=true;
			}

		}
		if(early.getListSubJobs().size()==2) { // Depot- patient- homeCareStaff-Patient
			// checking respect to the patient
			distanceAccum=inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1, late.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(late.getListSubJobs().get(0).getId()-1,early.getListSubJobs().get(1).getId()-1);
			String key=early.getListSubJobs().get(0).getSubJobKey()+early.getListSubJobs().get(1).getSubJobKey();
			toCheck=early.getDirectoryConnections().get(key);
			boolean patient=false;
			boolean client=false;
			if(toCheck.getDetour()>distanceAccum) {
				patient=true;
			}
			// checking respect to the client
			distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);
			if(toCheck.getDetour()>distanceAccum) {
				client=true;
			}

			if(patient && client) {
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean detourClients(Parts early, Parts late, SubJobs depot) {
		// son sólo dos clientes
		boolean detour=false;
		double distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);
		Edge toCheck=new Edge(depot,late.getListSubJobs().get(0),inp,test);
		if(toCheck.getDetour()>distanceAccum) {
			detour=true;
		}
		return detour;
	}

	private int typeOfParts(Parts parts, Parts parts2) {
		// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare 
		int typeParts=-1;

		if(parts.getQualificationLevel()>0 && parts2.getQualificationLevel()==0 ||parts2.getQualificationLevel()>0 && parts.getQualificationLevel()==0) {
			typeParts=2;
		}
		else {
			if(parts.getQualificationLevel()>0 && parts2.getQualificationLevel()>0) {

				typeParts=3;
			}	
			else {
				if(parts.getQualificationLevel()==0 && parts2.getQualificationLevel()==0) {
					typeParts=1;
				}	
			}
		}
		return typeParts;
	}



	private boolean insertingSubjobs(SubJobs j0, SubJobs j1, Parts pref, Parts psplit) {
		boolean instert=false;
		if(j0.getArrivalTime()<j1.getArrivalTime()) {
			double tv=inp.getCarCost().getCost(j0.getId()-1, j1.getId()-1);
			double arrivalTime=j0.getDepartureTime()+tv;



			if(j1.getArrivalTime()>arrivalTime) { //che evaluar si es posible llegar a tiempo


			}
		}
		return instert;
	}

	//	private Solution interMergingParts(Solution sol) {
	//		// tengo que asegurar que la jornada laboral destinada al conductor sea mayor que la máxima jornada permitida
	//		Solution copySolution=new Solution(sol);
	//		//System.out.println(copySolution.toString());
	//		for(int route1=0;route1<copySolution.getRoutes().size();route1++) {
	//			for(int route2=0;route2<copySolution.getRoutes().size();route2++) {
	//				int part=0;
	//				Route vehicle= new Route();
	//				Route iR=copySolution.getRoutes().get(route1);
	//
	//
	//				Route jR=copySolution.getRoutes().get(route2);
	//				if(iR.getPartsRoute().size()>2 && jR.getPartsRoute().size()>2) {
	//					if(possibleMerge(iR,jR)) {
	//						Route refRoute=selecctingStartRoute(iR,jR);
	//						Route toInsertRoute=selecctingRouteToInsert(iR,jR);
	//						ArrayList<SubJobs> list = selectingSubJobs(refRoute,toInsertRoute);
	//						boolean merging= false;
	//						if(vehicleCapacityPart(list)) {
	//							merging=true;
	//							for(SubJobs j: list) {
	//								vehicle.getSubJobsList().add(j);
	//							}
	//							settingParts(vehicle);
	//						}
	//						if(merging) {
	//							refRoute=updatingNewRoutes(refRoute,vehicle,part);	
	//							toInsertRoute=updatingChangedRoutes(toInsertRoute,refRoute,part);	
	//							vehicle.getPartsRoute().clear();
	//						}
	//						//System.out.println("Route refRoute"+ refRoute.toString());
	//						//System.out.println("Route toInsertRoute"+ toInsertRoute.toString());
	//						//System.out.println("Route toInsertRoute");
	//						//routeVehicleList.add(vehicle);
	//					}
	//				}
	//				else {
	//					break;
	//				}
	//			}
	//		}
	//		//System.out.println(copySolution.toString());
	//		settingSolution(copySolution);
	//
	//		return copySolution;
	//	}

	private Solution interMergingParts(Solution sol) {
		// tengo que asegurar que la jornada laboral destinada al conductor sea mayor que la máxima jornada permitida
		Solution copySolution=new Solution(sol);
		//System.out.println(copySolution.toString());
		for(int route1=0;route1<copySolution.getRoutes().size();route1++) {
			for(int route2=0;route2<copySolution.getRoutes().size();route2++) {
				int part=0;
				int start=2;
				Route vehicle= new Route();
				Route iR=copySolution.getRoutes().get(route1);
				if(iR.getPartsRoute().size()>2) {
					Route jR=copySolution.getRoutes().get(route2);
					//					//System.out.println("\nRoute iR"+ iR.toString());
					//					//System.out.println("\nRoute jR"+ jR.toString());
					if(possibleMerge(iR,jR)) {
						if(iR.getJobsDirectory().containsKey("D70") || jR.getJobsDirectory().containsKey("D70")) {
							//System.out.println("\nRoute iR"+ iR.toString());
							//System.out.println("\nRoute jR"+ jR.toString());
						}
						//						if(iR.getJobsDirectory().containsKey("P6") || jR.getJobsDirectory().containsKey("P6")) {
						//							//System.out.println("\nRoute iR"+ iR.toString());
						//							//System.out.println("\nRoute jR"+ jR.toString());
						//						}
						Route refRoute=selecctingStartRoute(iR,jR);
						Route toInsertRoute=selecctingRouteToInsert(iR,jR);
						//						if(refRoute.getIdRoute()==1 && toInsertRoute.getIdRoute()==22) {
						//							//System.out.println("\nRoute refRoute"+ refRoute.toString());
						//							//System.out.println("\nRoute toInsertRoute"+ toInsertRoute.toString());
						//						}
						//						//System.out.println("\nRoute refRoute"+ refRoute.toString());
						//						//System.out.println("\nRoute toInsertRoute"+ toInsertRoute.toString());
						//						if(refRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==21 && toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==7) {
						//							//System.out.println("\nRoute iR"+ iR.toString());
						//							//System.out.println("\nRoute jR"+ jR.toString());
						//						}
						if(toInsertRoute.getIdRoute()==5 || refRoute.getIdRoute()==5) {
							//System.out.println("\nRoute toInsertRoute"+ toInsertRoute.toString());
							//System.out.println("\nRoute refRoute"+ refRoute.toString());
						}
						boolean inserted=insertionAllRoute(refRoute,toInsertRoute,vehicle,part);
						if(!inserted) { // insertion part by part
							vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(0));
							vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(1));
							vehicle.setAmountParamedic(refRoute.getAmountParamedic());
							vehicle.setHomeCareStaff(refRoute.getHomeCareStaff());
							for(part=start;part<refRoute.getPartsRoute().size()-1;part++) {
								int newStart=0;
								inserted=isertingRoute(vehicle,toInsertRoute,refRoute, part);
								if(!inserted) { // insert part by part
									vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part));
									vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
								}
								else {
									newStart=part-1;
									part=newStart; // para que siga metiendo las partes de la ruta que aún falta por incorporar
								}

							}
							if(part==refRoute.getPartsRoute().size()-1 && !inserted ) {
								// en el caso de que no se haya terminado de insertar el resto
								boolean insertRest=isertingTheRest(vehicle,toInsertRoute);
								if(!insertRest) {
									vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part));
									vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
								}
							}
						}
						//						refRoute=updatingNewRoutes(refRoute,vehicle,part);	
						//						toInsertRoute.updateRouteFromParts(inp,test);
						refRoute=updatingNewRoutes(refRoute,vehicle,part);	
						toInsertRoute=updatingChangedRoutes(toInsertRoute,refRoute,part);	
						vehicle.getPartsRoute().clear();

						//System.out.println("Route refRoute"+ refRoute.toString());
						//System.out.println("Route toInsertRoute"+ toInsertRoute.toString());
						//System.out.println("Route toInsertRoute");
						//routeVehicleList.add(vehicle);
					}
				}
				else {
					break;
				}
			}
			//updatingListRoutes();
			//			//System.out.println(" Route list ");
			//			for(Route r:routeVehicleList) {
			//				//System.out.println(r.toString());
			//			}
		}
		//System.out.println(copySolution.toString());
		settingSolution(copySolution);

		return copySolution;
	}




	private void settingSolution(Solution copySolution) {
		LinkedList<Route> list= new LinkedList<Route>();
		for(Route r:copySolution.getRoutes()) {
			if(r.getPartsRoute().size()>2) {
				list.add(r);
			}
		}
		copySolution.getRoutes().clear();
		for(Route r:list) {
			copySolution.getRoutes().add(r);
		}
	}


	private void transferPartsInformation(ArrayList<Route> routeVehicleList2) {
		for(Route r:routeVehicleList2 ) {
			r.getSubJobsList().clear();
			for(int part=1;part<r.getPartsRoute().size()-1;part++) {
				for(SubJobs j:r.getPartsRoute().get(part).getListSubJobs()) {
					r.getSubJobsList().add(j);
				}
				r.getPartsRoute().get(part).settingConnections(r.getPartsRoute().get(part).getListSubJobs(), inp, test);
			}
		}
	}

	private boolean insertionAllRoute(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean feasibleTimes= false;
		if(capacityVehicle(refRoute,toInsertRoute)) {	
			feasibleTimes=checkingTimeWindows(refRoute,toInsertRoute,vehicle,part);
		}
		//}	
		return feasibleTimes;
	}

	private boolean capacityVehicle(Route refRoute, Route toInsertRoute) {
		boolean enoughCapacity=false;
		int passengers=0;
		int action=0;
		for(int i=0;i<refRoute.getPartsRoute().size()-1;i++) { // route 1
			Parts r=refRoute.getPartsRoute().get(i);
			for(SubJobs s:r.getListSubJobs()) {
				action=s.getTotalPeople();
				passengers+=action;
				if(Math.abs(passengers)>inp.getVehicles().get(0).getMaxCapacity()) {
					enoughCapacity=false	;
					break;
				}
			}
		}
		for(int i=0;i<toInsertRoute.getPartsRoute().size()-1;i++) { // route 2
			Parts r=toInsertRoute.getPartsRoute().get(i);
			for(SubJobs s:r.getListSubJobs()) {
				action=s.getTotalPeople();
				passengers+=action;
				if(Math.abs(passengers)>inp.getVehicles().get(0).getMaxCapacity()) {
					enoughCapacity=false	;
					break;
				}
			}
		}
		if(Math.abs(passengers)>inp.getVehicles().get(0).getMaxCapacity()) {
			enoughCapacity=false	;
		}
		else {
			enoughCapacity=true	
					;}
		return enoughCapacity;
	}

	private boolean checkingTimeWindows(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean inserted=keeptheTimes(refRoute,toInsertRoute,vehicle,part);
		if(!inserted) {
			inserted=keeptheTimesInfeasible(refRoute,toInsertRoute,vehicle,part);}
		return inserted;
	}


	private boolean keeptheTimesInfeasible(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean inserted=false;
		int lastPart=refRoute.getPartsRoute().size()-1;// <- esta parte hace referencia al depot
		//	.getListSubJobs().get(0);
		Parts partEnd=refRoute.getPartsRoute().get(lastPart-1);
		SubJobs lastSubjobInRoute= partEnd.getListSubJobs().get(partEnd.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);
		if(lastSubjobInRoute.getDepartureTime()<firstSubjob.getArrivalTime()) {
			part=refRoute.getPartsRoute().size();
			inserted=true;
			for(int j=0;j<refRoute.getPartsRoute().size()-1;j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(j));
			}
			for(int j=1;j<toInsertRoute.getPartsRoute().size();j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(j));
			}
			vehicle.setAmountParamedic(refRoute.getAmountParamedic()+toInsertRoute.getAmountParamedic());
			vehicle.setHomeCareStaff(refRoute.getHomeCareStaff()+toInsertRoute.getHomeCareStaff());
			vehicle.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople((int)(vehicle.getAmountParamedic()+vehicle.getHomeCareStaff()));
			vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		}
		return inserted;
	}



	private boolean keeptheTimes(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean inserted=false;
		int lastPart=refRoute.getPartsRoute().size()-1;// <- esta parte hace referencia al depot
		//	.getListSubJobs().get(0);
		Parts partEnd=refRoute.getPartsRoute().get(lastPart-1);
		SubJobs lastSubjobInRoute= partEnd.getListSubJobs().get(partEnd.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
		if((lastSubjobInRoute.getDepartureTime()+tv)<firstSubjob.getArrivalTime()) {
			part=refRoute.getPartsRoute().size();
			inserted=true;
			for(int j=0;j<refRoute.getPartsRoute().size()-1;j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(j));

			}
			for(int j=1;j<toInsertRoute.getPartsRoute().size();j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(j));
			}
			vehicle.setAmountParamedic(refRoute.getAmountParamedic()+toInsertRoute.getAmountParamedic());
			vehicle.setHomeCareStaff(refRoute.getHomeCareStaff()+toInsertRoute.getHomeCareStaff());
			vehicle.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople((int)(vehicle.getAmountParamedic()+vehicle.getHomeCareStaff()));
			vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		}
		return inserted;
	}








	private Route updatingNewRoutes(Route refRoute, Route vehicle, int part) {
		// el objetivo es dejar que la ruta refRoute sólo tenga partes contenidas en la ruta de vehicle
		refRoute.getPartsRoute().clear();
		for(Parts p:vehicle.getPartsRoute()) {// no se considera el depot
			refRoute.getPartsRoute().add(p);
		}
		refRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		int totalPeople=vehicle.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople();
		refRoute.setAmountParamedic(vehicle.getAmountParamedic());
		refRoute.setHomeCareStaff(vehicle.getHomeCareStaff());
		refRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(totalPeople);
		return refRoute;
	}

	private Route updatingChangedRoutes(Route refRoute, Route vehicle, int part) {
		ArrayList<Parts> toKeep= new ArrayList<Parts>();
		for(Parts p:refRoute.getPartsRoute()) {
			toKeep.add(p);
		}
		for(Parts p:toKeep) {
			for(SubJobs s:p.getListSubJobs()) {
				if(s.getId()!=1) {
					if(vehicle.getJobsDirectory().containsKey(s.getSubJobKey())) {
						refRoute.getPartsRoute().remove(p);
						break;
					}
				}
			}

		}
		refRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		return refRoute;
	}
	private boolean isertingRoute(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
		boolean inserted=keepAStartServiceTime(vehicle,toInsertRoute,refRoute,part2);
		if(!inserted) {
			inserted=keepAStartServiceTimeInfeasible(vehicle,toInsertRoute,refRoute,part2);}
		return inserted; 
	}

	private boolean keepAStartServiceTimeInfeasible(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		for(int i=1;i<toInsertRoute.getPartsRoute().size()-1;i++) { // -1 para no incluir el depot al final
			// primer intenta insertar la parte de la otra ruta

			Parts endPart=vehicle.getPartsRoute().getLast();
			SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
			SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(0);
			SubJobs lastSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(toInsertRoute.getPartsRoute().get(i).getListSubJobs().size()-1);
			SubJobs nextSubJobPart= refRoute.getPartsRoute().get(part2).getListSubJobs().get(0);
			//double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
			//boolean nextPart= checkingNextPart(lastSubjobInRoute,firstSubjob,lastSubjob,nextSubJobPart,refRoute.getPartsRoute().get(part2),toInsertRoute.getPartsRoute().get(i),vehicle);


			if((lastSubjobInRoute.getDepartureTime())<firstSubjob.getArrivalTime() ) {
				//	if((firstSubjob.getArrivalTime()-(lastSubjobInRoute.getDepartureTime()+tv))<=test.getCumulativeWaitingTime()) {
				if(vehicleCapacity(refRoute,part2,toInsertRoute.getPartsRoute().get(i))) {
					if(lastSubjobInRoute.getDepartureTime()<firstSubjob.getArrivalTime() && lastSubjob.getDepartureTime()<nextSubJobPart.getArrivalTime()) {
						vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
						if(i==1) {
							//if(refRoute.getPartsRoute().get(part2).getListSubJobs().get(0).getId()==1) {
							//if(i==1) {
							double personnel1=	refRoute.getAmountParamedic();
							vehicle.setAmountParamedic(personnel1+toInsertRoute.getAmountParamedic());
							personnel1=refRoute.getHomeCareStaff();
							vehicle.setHomeCareStaff(personnel1+toInsertRoute.getHomeCareStaff());

							vehicle.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople((int)(vehicle.getAmountParamedic()+vehicle.getHomeCareStaff()));

							//}
							//vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part2));
							part2++;
						}
						vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
						//System.out.println("Printing changing 2 " +vehicle.toString());
						changing.removingParts(toInsertRoute.getPartsRoute().get(i));
						changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
						//System.out.println("Printing changing 1 " +changing.toString());
						//System.out.println("Printing vehicleRouting" +vehicle.toString());
						inserted=true;	
						break;	
					}				
				}
			}
			if(!inserted) {
				break;
			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		//System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted;
	}



	//	private boolean keepAStartServiceTime(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
	//		boolean inserted=false;		
	//		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
	//		if(toInsertRoute.getPartsRoute().size()>2) {
	//			Route changing= new Route(toInsertRoute);
	//			// primer intenta insertar la parte de la otra ruta
	//			mergingPart(toInsertRoute.getPartsRoute().get(1),refRoute.getPartsRoute().get(part2),vehicle,inserted);
	//			vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
	//			//System.out.println("Printing changing 2 " +vehicle.toString());
	//			changing.removingParts(toInsertRoute.getPartsRoute().get(1));
	//			changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
	//			//System.out.println("Printing changing 1 " +changing.toString());
	//			//System.out.println("Printing vehicleRouting" +vehicle.toString());
	//			inserted=true;	
	//
	//			toInsertRoute.getPartsRoute().clear();
	//			for(Parts part:changing.getPartsRoute()) {
	//				toInsertRoute.getPartsRoute().add(part);
	//			}
	//			toInsertRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
	//			//System.out.println("Printing toInsertRoute" +toInsertRoute.toString());}
	//		return inserted;
	//	}


	private boolean keepAStartServiceTime(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		for(int i=1;i<toInsertRoute.getPartsRoute().size()-1;i++) { // -1 para no incluir el depot al final
			// primer intenta insertar la parte de la otra ruta

			Parts endPart=vehicle.getPartsRoute().getLast();




			SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
			SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(0);
			SubJobs lastSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(toInsertRoute.getPartsRoute().get(i).getListSubJobs().size()-1);
			SubJobs nextSubJobPart= refRoute.getPartsRoute().get(part2).getListSubJobs().get(0);
			double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
			boolean nextPart= checkingNextPart(lastSubjobInRoute,firstSubjob,lastSubjob,nextSubJobPart,refRoute.getPartsRoute().get(part2),toInsertRoute.getPartsRoute().get(i),vehicle);
			if((lastSubjobInRoute.getDepartureTime()+tv)<firstSubjob.getArrivalTime() && nextPart) {
				if((firstSubjob.getArrivalTime()-(lastSubjobInRoute.getDepartureTime()+tv))<=test.getCumulativeWaitingTime()) {
					if(vehicleCapacity(refRoute,part2,toInsertRoute.getPartsRoute().get(i))) {
						vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
						if(i==1) {
							//if(refRoute.getPartsRoute().get(part2).getListSubJobs().get(0).getId()==1) {
							//if(i==1) {
							double personnel1=	refRoute.getAmountParamedic();
							vehicle.setAmountParamedic(personnel1+toInsertRoute.getAmountParamedic());
							personnel1=refRoute.getHomeCareStaff();
							vehicle.setHomeCareStaff(personnel1+toInsertRoute.getHomeCareStaff());

							vehicle.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople((int)(vehicle.getAmountParamedic()+vehicle.getHomeCareStaff()));

							//}
							//vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part2));
							part2++;
						}
						vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
						//System.out.println("Printing changing 2 " +vehicle.toString());
						changing.removingParts(toInsertRoute.getPartsRoute().get(i));
						changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
						//System.out.println("Printing changing 1 " +changing.toString());
						//System.out.println("Printing vehicleRouting" +vehicle.toString());
						inserted=true;	
						break;	
					}				
				}
			}
			if(!inserted) {
				break;
			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		//System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted;
	}

	private boolean vehicleCapacity(Route ref,int insertionPosition, Parts parts2) {
		boolean capacityEnough=false;
		int passenger=0;
		int action=0;
		for(int i=0;i<ref.getPartsRoute().size();i++) {
			if(i==insertionPosition) {
				for(SubJobs s:parts2.getListSubJobs()) {
					action=s.getTotalPeople();
					passenger+=action;
				}	
				for(SubJobs s:ref.getPartsRoute().get(i).getListSubJobs()) {
					action=s.getTotalPeople();
					passenger+=action;
				}
				if(Math.abs(passenger)>inp.getVehicles().get(0).getMaxCapacity()){
					capacityEnough=false;
					break;
				}
				else {
					capacityEnough=true;
				}
			}
			else {
				for(SubJobs s:ref.getPartsRoute().get(i).getListSubJobs()) {
					action=s.getTotalPeople();
					passenger+=action;
				}
				if(Math.abs(passenger)>inp.getVehicles().get(0).getMaxCapacity()){
					capacityEnough=false;
					break;
				}
				else {
					capacityEnough=true;
				}
			}
		}

		if(Math.abs(passenger)>inp.getVehicles().get(0).getMaxCapacity()){
			capacityEnough=false;
		}
		else {
			capacityEnough=true;
		}
		return capacityEnough;
	}

	private boolean isertingTheRest(Route vehicle, Route toInsertRoute) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		Parts endPart=vehicle.getPartsRoute().getLast();
		SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);

		// primer intenta insertar la parte de la otra ruta

		if(lastSubjobInRoute.getDepartureTime()<firstSubjob.getArrivalTime()) {
			for(int i=1;i<toInsertRoute.getPartsRoute().size();i++) { // -1 para no incluir el depot al final
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
				vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
				//System.out.println("Printing changing 2 " +vehicle.toString());
				changing.removingParts(toInsertRoute.getPartsRoute().get(i));
				changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
				//System.out.println("Printing changing 1 " +changing.toString());
				//System.out.println("Printing vehicleRouting" +vehicle.toString());
				inserted=true;	

			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		//System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted; 
	}
	//boolean nextPart= checkingNextPart(lastSubjobInRoute,firstSubjob,lastSubjob,nextSubJobPart,refRoute.getPartsRoute().get(part2),toInsertRoute.getPartsRoute().get(i),vehicle);

	private boolean checkingNextPart(SubJobs lastSubjobInRoute, SubJobs firstSubjob, SubJobs lastSubjob, SubJobs nextSubJobPart, Parts nextPartInsert, Parts toInsert, Route vehicle) {
		// structure route ref part 1 {1,...,lastSubjobInRoute} part 2{nextSubJobPart, } route to insert part 1 {firstSubjob,...,lastSubjob}
		boolean nextPart=false;
		double tvlastSubjobInRoutefirstSubjob=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
		double tvlastSubjobnextSubJobPart =inp.getCarCost().getCost(lastSubjob.getId()-1, nextSubJobPart.getId()-1);
		if(lastSubjobInRoute.getId()==1) {
			nextPart=true;
		}
		else {

			if(lastSubjobInRoute.getDepartureTime()+tvlastSubjobInRoutefirstSubjob<firstSubjob.getArrivalTime() && lastSubjob.getDepartureTime()+tvlastSubjobnextSubJobPart<nextSubJobPart.getArrivalTime()) {
				nextPart=true;
			}
			//			else { // intentar cambiar la hora del firstSubjob
			//				// se cambia la hora tomando como referencia el trabajo futuro
			//				double registrationTime=0;
			//				double loadUnloadTime=0;
			//				if(firstSubjob.isClient()) { // si es un cliente 
			//					loadUnloadTime=test.getloadTimeHomeCareStaff();
			//				}
			//				else {
			//					loadUnloadTime=test.getloadTimePatient();
			//					registrationTime=test.getRegistrationTime();
			//				}
			//				double departureTimefirstSubjob=nextSubJobPart.getArrivalTime()-tvlastSubjobnextSubJobPart;
			//				double arrivalTime=departureTimefirstSubjob-loadUnloadTime;
			//				double startServiceTime=arrivalTime+registrationTime;
			//				if(startServiceTime>=firstSubjob.getStartTime() && startServiceTime<=firstSubjob.getEndTime()) {
			//					firstSubjob.setStartServiceTime(startServiceTime);
			//					firstSubjob.setEndServiceTime(firstSubjob.getsortLTWSizeCriterion()+firstSubjob.getReqTime());
			//					firstSubjob.setarrivalTime(arrivalTime);
			//					firstSubjob.setdepartureTime(departureTimefirstSubjob);
			//					nextPart=true;
			//				}
			//
			//			}
		}
		return nextPart;
	}


	private Route selecctingRouteToInsert(Route iR, Route jR) {
		//System.out.println("printing part IR");
		printing(iR.getPartsRoute().get(0));
		printing(iR.getPartsRoute().get(1));
		//System.out.println("printing parts JR");
		printing(jR.getPartsRoute().get(0));
		printing(jR.getPartsRoute().get(1));


		if(iR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()> jR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()) {
			return iR;
		}
		else {
			return jR;
		}
	}

	private Route selecctingStartRoute(Route iR, Route jR) {
		//System.out.println("printing part IR");
		//System.out.println("part 1");
		printing(iR.getPartsRoute().get(0));
		//System.out.println("part 2");
		printing(iR.getPartsRoute().get(1));
		//System.out.println("printing parts JR");
		printing(jR.getPartsRoute().get(0));
		printing(jR.getPartsRoute().get(1));
		if(iR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()> jR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()) {
			return jR;
		}
		else {
			return iR;
		}
	}

	private boolean possibleMerge(Route iR, Route jR) {
		// Revisar que las rutas que se van a mezclar no sean las mismas rutas
		boolean routeToRemve=false;
		boolean merging=false;
		boolean diffRoute=false;
		boolean capacityVehicle=false;
		boolean driverRouteLength=false;
		if(iR.getPartsRoute().size()>2 && jR.getPartsRoute().size()>2) {
			routeToRemve=true;
		}
		if(iR!=jR ) {
			//if(iR.getHomeCareStaff()==jR.getHomeCareStaff() && iR.getAmountParamedic()==jR.getAmountParamedic()) {
			diffRoute=true;	
			//}
		}
		// Revisar que el número de personas en el auto no excedan de la capacidad del
		double totalPassenger=iR.getAmountParamedic()+iR.getHomeCareStaff()+jR.getAmountParamedic()+jR.getHomeCareStaff();
		if(totalPassenger < inp.getVehicles().get(0).getMaxCapacity()) {
			capacityVehicle=true;
		}
		if((iR.getDurationRoute()+jR.getDurationRoute())<=test.getRouteLenght()) {
			//if((iR.getTravelTime()+iR.getloadUnloadRegistrationTime()+jR.getTravelTime()+jR.getloadUnloadRegistrationTime())<=test.getRouteLenght()) {
			driverRouteLength=true;
		}

		if(capacityVehicle && diffRoute && routeToRemve && driverRouteLength) {
			merging=true;
		}
		return merging;
	}



	private Solution solutionInformation(ArrayList<Route> routeList2) {
		Solution initialSol= new Solution();
		int routeN=-1;

		for(Route r:routeList2 ) {
			routeN++;
			if(!r.getSubJobsList().isEmpty()) {
				//System.out.println(r.toString());
				r.updateRouteFromParts(inp,test,jobsInWalkingRoute);
				//System.out.println(r.toString());
				r.setIdRoute(routeN);
				initialSol.getRoutes().add(r);
			}
		}
		//System.out.println(initialSol);
		// Computar costos asociados a la solucion
		//computeSolutionCost(initialSol);
		//System.out.println(initialSol);
		// la lista de trabajos asociados a la ruta

		transferPartsInformation(routeList2);

		// list passengers
		double paramedic=0;
		double homeCoreStaff=0;

		for(Route r:routeList2) {
			paramedic+=r.getAmountParamedic();
			homeCoreStaff+=r.getHomeCareStaff();

		}
		initialSol.setHomeCareStaff(homeCoreStaff);
		initialSol.setParamedic(paramedic);
		return initialSol;
	}


	private double computeAdditionalTime(SubJobs firtSubJobToInsert) {
		double additionalTime=0;
		if(firtSubJobToInsert.isClient()) {
			additionalTime=test.getloadTimeHomeCareStaff();
		}
		else {
			if(firtSubJobToInsert.isMedicalCentre()) {
				if(firtSubJobToInsert.getTotalPeople()<0) {// drop-off at medical centre
					additionalTime=test.getloadTimePatient()+test.getRegistrationTime();
				}
				else {// pick-up at medical centre
					additionalTime=test.getloadTimePatient();
				}			
			}
			else { // pick up or drop-ff at patient home
				additionalTime=test.getloadTimePatient();
			}
		}
		return additionalTime;
	}



	private void readingTurns(ArrayList<Schift> sortedSchift, Schift turn) {
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es más tardío el el inicio del trabajo de part
			SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(0).getRoute().getSubJobsList().get(0);
			if(firstTurnInList.getstartServiceTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {
				sortedSchift.add(0,turn);
			}
		}
		else {
			int lastTurn=sortedSchift.size()-1;
			SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(0).getRoute().getSubJobsList().get(0);
			SubJobs lastTurnInList=sortedSchift.get(lastTurn).getRoute().getSubJobsList().get(0);
			if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
				sortedSchift.add(0,turn);
			}
			else {				
				// 1. si es más temprano que el primer trabajo
				if(lastTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
					sortedSchift.add(turn);
				}
				else {
					readingArray(sortedSchift,turn);			
				}
			}
		}
	}


	private void readingTailsTurns(ArrayList<Schift> sortedSchift, Schift turn) {
		int lastJobToInsert=turn.getRoute().getSubJobsList().size();
		SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(lastJobToInsert-1);
		int lastJobInRoute=sortedSchift.get(0).getRoute().getSubJobsList().size();
		SubJobs firstTurnInList=sortedSchift.get(0).getRoute().getSubJobsList().get(lastJobInRoute-1);
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es más tardío el el inicio del trabajo de part
			if(firstTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {
				sortedSchift.add(0,turn);
			}
		}
		else {
			int lastTurn=sortedSchift.size()-1;
			lastJobInRoute=sortedSchift.get(lastTurn).getRoute().getSubJobsList().size();
			SubJobs lastTurnInList=sortedSchift.get(lastTurn).getRoute().getSubJobsList().get(lastJobInRoute-1);
			if(lastTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {				
				// 1. si es más temprano que el primer trabajo
				if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
					sortedSchift.add(turn);
				}
				else {
					readingArray(sortedSchift,turn);			
				}
			}
		}
	}





	private ArrayList<ArrayList<SubJobs>> partConnectedToDepot(ArrayList<ArrayList<SubJobs>> partsToInsert) {
		ArrayList<ArrayList<SubJobs>> tailRoutes=new ArrayList<ArrayList<SubJobs>>();// order
		for(ArrayList<SubJobs> part:partsToInsert) {
			if(part.get(part.size()-1).getId()==1) {
				tailRoutes.add(part);	
			}				
		}
		return tailRoutes;
	}

	private ArrayList<ArrayList<SubJobs>> partConnectedFromDepot(ArrayList<ArrayList<SubJobs>> partsToInsert) {
		ArrayList<ArrayList<SubJobs>> headRoutes=new ArrayList<ArrayList<SubJobs>>();// order
		for(ArrayList<SubJobs> part:partsToInsert) {
			if(part.get(0).getId()==1) {
				headRoutes.add(part);	
			}				
		}
		return headRoutes;
	}


	private void printingTurn(Schift s) {
		int i=-1 ;// part of the turn
		for(Parts j:s.getRouteParts()) {	
			i++;
			//System.out.println("Part "+ i);
			for(SubJobs jb:j.getListSubJobs()) {
				//System.out.println("subjob "+ jb.getSubJobKey()+ "arival "+ jb.getArrivalTime()+ "Start_service " + jb.getstartServiceTime()+ "  " );
			}
		}

	}


	private void readingArray(ArrayList<Schift> sortedSchift, Schift turn) {
		for(int i=1;i<sortedSchift.size();i++) {
			SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(i).getRoute().getSubJobsList().get(0);
			if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
				SubJobs previousTurnInList=sortedSchift.get(i-1).getRoute().getSubJobsList().get(0);
				if(previousTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
					sortedSchift.add(i,turn);
					break;
				}
			}
		}
	}


	private Solution assigmentJobsToVehicles(ArrayList<ArrayList<Couple>> clasification) { 
		// solo la asignación de trabajos a los niveles de calificaciones // se hace una secuencia ordenad_
		//  no se consideran los downgradings solo se asignan tareas de acuerdo con la compatibilidad de
		// de las ventanas de tiempo
		// 1. Clasification de trabajos de acuerdo a las qualificaciones
		//1. list of jobs
		// Classifying clients: Home care staff
		ArrayList<Jobs> clasification3 = creationJobsHomeCareStaff(clasification.get(0)); 
		clasification3.sort(Jobs.TWSIZE_Early);
		ArrayList<Jobs> clasification2 = creationJobsHomeCareStaff(clasification.get(1));
		clasification2.sort(Jobs.TWSIZE_Early);
		ArrayList<Jobs> clasification1 = creationJobsHomeCareStaff(clasification.get(2));
		clasification1.sort(Jobs.TWSIZE_Early);
		// Classifying patients: Paramedics
		ArrayList<Jobs> clasification0 = creationJobsParamedics(clasification.get(3));
		clasification0.sort(Jobs.TWSIZE_Early);
		// 2. Calling the type and quantity of home care staff
		List<AttributeNurse> homeCareStaff= inp.gethomeCareStaffInf(); // home Care Staff according the qualification level
		List<AttributeParamedics> paramedic= inp.getParamedic(); // paramedic qualification level
		// Home care staff for:
		int q3= homeCareStaff.get(2).getQuantity(); // Qualification 3
		int q2= homeCareStaff.get(1).getQuantity(); // Qualification 2
		int q1= homeCareStaff.get(0).getQuantity(); // Qualification 1
		int q0= paramedic.get(0).getQuantity(); // Qualification 0

		// 3. Definition of a feasible sequence of jobs for each qualification level
		//	ArrayList<ArrayList<Jobs>> qualification3= assigmentHighQualification(q3,clasification3);

		//Qualification level =0
		ArrayList<Parts> qualification01= assigmentPatientsDropOff(q0,clasification0);
		missingAssigment(qualification01,clasification0);

		assigmentClientsDropOff(q0,clasification3);
		missingAssigment(qualification01,clasification3);

		assigmentClientsDropOff(q0,clasification2);
		missingAssigment(qualification01,clasification2);

		assigmentClientsDropOff(q0,clasification1);
		missingAssigment(qualification01,clasification1);
		ArrayList<Parts> partsList = new ArrayList<>();

		for(Parts p: sequenceVehicles) {
			if(!p.getListSubJobs().isEmpty()) {
				partsList.add(p);
				//System.out.println(p.toString());
			}
		}
		sequenceVehicles.clear();
		for(Parts p: partsList) {			
			sequenceVehicles.add(p);
		}


		ArrayList<Route> route=insertingDepotConnections(sequenceVehicles);
		// creación de partes
		Solution newSol= solutionInformation(route); 

		newSol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);



		//System.out.println(newSol.toString());
		Solution mergingRoutes= checkingMergingRoutes(newSol);
		mergingRoutes.checkingSolution(inp, test, jobsInWalkingRoute,initialSol);
		//System.out.println("Stop");
		//System.out.println(newSol.toString());


		return mergingRoutes;

	}

	private Solution checkingMergingRoutes(Solution newSol) {
		Solution s= new Solution(newSol);
		ArrayList<Route> routeCopy=new ArrayList<Route>();
		for(Route r: s.getRoutes()) {
			routeCopy.add(r);
		}
		routeCopy.sort(Route.SORT_BY_RouteLength);



		for(Route routeSol:s.getRoutes()) {
			for(Route r:routeCopy) {
				if(!r.getSubJobsList().isEmpty()) {
					Route mergingRoute=mergingRoutes(routeSol,r);
					//System.out.println("merging :" +mergingRoute.toString());		
					if(!r.getSubJobsList().isEmpty()) {
						//System.out.println(routeSol.toString());
					}
				}
			}
		}
		routeCopy.clear();

		for(Route r:s.getRoutes() ) {
			if(!r.getSubJobsList().isEmpty()) {
				routeCopy.add(r);
			}
			//System.out.println(r.toString());
		}
		s.getRoutes().clear();
		for(Route r:routeCopy) {
			s.getRoutes().add(r);
			//System.out.println(r.toString());
		}
		return s;
	}

	private Route mergingRoutes(Route routeSol, Route r) {
		boolean merging= false;
		if(routeSol!=r) {
			ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>();
			for(SubJobs sj: routeSol.getSubJobsList()){
				listSubJobs.add(sj);
			}
			for(SubJobs sj: r.getSubJobsList()){
				listSubJobs.add(sj);
			}
			listSubJobs.sort(Jobs.SORT_BY_ARRIVALTIME);

			if(vehicleCapacityPart(listSubJobs) && checkingWorkingDriver(listSubJobs)) {
				for(int i=1;i<listSubJobs.size();i++) { // evaluar por parejas
					SubJobs jobi=listSubJobs.get(i-1);
					SubJobs jobj=listSubJobs.get(i);

					double tvjobij=inp.getCarCost().getCost(jobi.getId()-1, jobj.getId()-1);
					if((jobi.getDepartureTime()+tvjobij)<=jobj.getArrivalTime()) {
						merging=true;
					}
					else {
						merging=false;
						break;
					}
				}

			}
			//routeSol.getPartsRoute().clear();

			if(merging) {
				r.getSubJobsList().clear();
				routeSol.getSubJobsList().clear();
				for(SubJobs j: listSubJobs) {
					routeSol.getSubJobsList().add(j);
				}
				routeSol.updateRouteFromSubJobs(inp, test, jobsInWalkingRoute,routeSol.getPartsRoute().getFirst().getListSubJobs(),routeSol.getPartsRoute().getLast().getListSubJobs(),listSubJobs);

				SubJobs firstJob=routeSol.getSubJobsList().get(0); // it is not the depot node
				computeStartTimeRoute(firstJob,routeSol);
				//r.getSubJobsList().get(0).setserviceTime(0);
				// end time
				SubJobs lastJob=routeSol.getSubJobsList().get(routeSol.getSubJobsList().size()-1);  // it is not the depot node
				computeEndTimeRoute(lastJob,routeSol);
				//System.out.println(routeSol.toString());
			}

		}
		return routeSol;
	}

	private void assigmentClientsDropOff(int q0, ArrayList<Jobs> clasification3) {
		ArrayList<SubJobs> listSubJobsDropOff= new ArrayList<SubJobs>();
		ArrayList<SubJobs> listSubJobsPickUp= new ArrayList<SubJobs>();
		HashMap<String,SubJobs> pickUpDirectory= new HashMap<>();
		for(Jobs j:clasification3) { // generating List of jobs
			if(j.getId()==21) {
				//System.out.println(j.toString());
			}
			Parts p=disaggregatedJob(j);
			//System.out.println(p.toString());
			SubJobs dropOffPatient=p.getListSubJobs().get(0);
			listSubJobsDropOff.add(dropOffPatient);
			SubJobs pickUpPatientMC=p.getListSubJobs().get(1);
			//listSubJobsPickUp.add(pickUpPatientMC);
			pickUpDirectory.put(pickUpPatientMC.getSubJobKey(), pickUpPatientMC);
		}
		listSubJobsDropOff.sort(Jobs.SORT_BY_STARTW);
		listSubJobsPickUp.sort(Jobs.SORT_BY_STARTW);

		for(SubJobs j:listSubJobsDropOff) {
			if(j.getSubJobKey().equals("D38")) {
				//System.out.println(j.toString());
			}
			if(j.getId()==21) {
				//System.out.println(j.toString());
			}
			boolean insertesed=false;
			boolean secondPart=false;
			//Couple c1= dropoffHomeCareStaff.get(j.getSubJobKey());
			Couple c= new Couple(dropoffHomeCareStaff.get(j.getSubJobKey()), inp,test);
			SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
			SubJobs pickUp=(SubJobs)c.getStartEndNodes().get(0);
			if(pickUp.getSubJobKey().equals("P38")) {
				//System.out.println(j.toString());
			}



			for(Parts paramedic:sequenceVehicles) {
				if(paramedic.getListSubJobs().isEmpty()) {

					insertesed=true;
					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(pickUp);
					pickUpDirectory.remove(pickUp.getSubJobKey());
					//System.out.println("Stop");
					break;
				}
				else { // iterating over the route
					insertesed=insertingPairSubJobsDropOffClient(present,paramedic);
					if(insertesed) {
						homeCarePickUp(present,pickUp);
						secondPart=insertingPairSubJobsPickUpClient(pickUp,paramedic);
						if(secondPart) {
							pickUpDirectory.remove(pickUp.getSubJobKey());
						}
						else {
							listSubJobsPickUp.add(pickUp);
						}
						break;
					}
				}
			}
			if(!insertesed) {
				if(present.getSubJobKey().equals("D26")) {
					//System.out.println("Stop");	
				}
				Parts newPart=new Parts();
				newPart.getListSubJobs().add(present);
				newPart.getListSubJobs().add(pickUp);
				pickUpDirectory.remove(pickUp.getSubJobKey());
				//System.out.println("Stop");
				sequenceVehicles.add(newPart);
				break;
			}
		}


		listSubJobsPickUp.sort(Jobs.SORT_BY_STARTW);

		for(SubJobs present:listSubJobsPickUp) {
			if(present.getSubJobKey().equals("P38")) {
				//System.out.println("Stop");
			}
			if(present.getId()==21) {
				//System.out.println("Stop");
			}
			//			Couple c= new Couple(pickUpHomeCareStaff.get(pickUp.getSubJobKey()), inp,test);
			//			SubJobs present=(SubJobs)c.getStartEndNodes().get(0);
			if(pickUpDirectory.containsKey(present.getSubJobKey())) {
				boolean insertesed=false;
				for(Parts paramedic:sequenceVehicles) {

					//SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
					if(paramedic.getListSubJobs().isEmpty()) {
						insertesed=true;
						paramedic.getListSubJobs().add(present);
						//System.out.println("Stop");
						break;
					}
					else { // iterating over the route
						insertesed=insertingPairSubJobsPickUpClient(present,paramedic);
						if(insertesed) {
							break;
						}
					}
				}
				if(!insertesed) {
					if(present.getSubJobKey().equals("P22")) {
						//System.out.println("Stop");
					}
					Parts newPart=new Parts();
					newPart.getListSubJobs().add(new SubJobs(present));
					//System.out.println("Stop");
					sequenceVehicles.add(newPart);
					//break;

				}
			}
		}

	}

	private ArrayList<Parts> assigmentPatientsDropOff(int q3, ArrayList<Jobs> clasification3) {
		sequenceVehicles= new ArrayList<> (q3);

		for(int i=0;i<q3;i++) {
			Parts newPart=new Parts();
			sequenceVehicles.add(newPart);
		}

		ArrayList<SubJobs> listSubJobsDropOff= new ArrayList<SubJobs>();
		ArrayList<SubJobs> listSubJobsPickUp= new ArrayList<SubJobs>();

		for(Jobs j:clasification3) { // generating List of jobs
			Parts p=disaggregatedJob(j);
			//System.out.println(p.toString());
			SubJobs dropOffPatient=p.getListSubJobs().get(1);
			listSubJobsDropOff.add(dropOffPatient);
			SubJobs pickUpPatientMC=p.getListSubJobs().get(2);
			listSubJobsPickUp.add(pickUpPatientMC);
		}

		listSubJobsDropOff.sort(Jobs.SORT_BY_STARTW);
		listSubJobsPickUp.sort(Jobs.SORT_BY_STARTW);

		for(SubJobs j:listSubJobsDropOff) {
			if(j.getSubJobKey().equals("D4871")) {
				//System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4972")) {
				//System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4669")) {
				//System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4456")) {
				//System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4463")) {
				//System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4954")) {
				//System.out.println("Stop");
			}
			Couple c= new Couple(dropoffpatientMedicalCentre.get(j.getSubJobKey()), inp,test);
			SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
			SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
			for(Parts paramedic:sequenceVehicles) {
				if(paramedic.getDirectoryConnections().containsKey("D4972")) {
					//System.out.println("Stop");
				}
				System.out.print("Stop");
				System.out.print(paramedic.toString());
				boolean insertesed=false;
				if(paramedic.getListSubJobs().isEmpty()) {
					insertesed=true;

					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(future);
					//System.out.println("Stop");
					break;
				}
				else { // iterating over the route
					insertesed=insertingPairSubJobsDropOffPickUpPatient(present,future,paramedic);
					System.out.print("After");
					System.out.print(paramedic.toString());
					if(insertesed) {
						break;
					}
				}
			}
		}

		for(SubJobs pickUp:listSubJobsPickUp) {
			boolean insertesed=false;
			if(pickUp.getSubJobKey().equals("P4871")) {
				//System.out.println("Stop");
			}
			for(Parts paramedic:sequenceVehicles) {
				System.out.print("before");
				System.out.print(paramedic.toString());
				if(paramedic.getListSubJobs().isEmpty()) {
					insertesed=true;
					Couple c= new Couple(pickpatientMedicalCentre.get(pickUp.getSubJobKey()), inp,test);
					SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
					SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(future);
					break;
				}
				else { // iterating over the route
					insertesed=insertingPairSubJobsPickUpDropOffPatient(pickUp,paramedic);
					System.out.print("After");
					System.out.print(paramedic.toString());
					if(insertesed) {
						break;
					}
				}
			}
		}


		return sequenceVehicles;
	}

	private boolean insertingPairSubJobsDropOffPickUpPatient(SubJobs present, SubJobs future, Parts paramedic) {
		boolean merge=false;
		boolean inserted=false;
		ArrayList<SubJobs> preliminarySubJobList= new ArrayList<>();
		inserted=iteratingOverSequenceSubJobsDP(preliminarySubJobList,present,future,paramedic,merge); // structure a---j---b
		if(!inserted) { // insertarlo al final
			SubJobs lastSubJobs=paramedic.getListSubJobs().get(paramedic.getListSubJobs().size()-1);
			double tvjlastSubJobs=inp.getCarCost().getCost(lastSubJobs.getId()-1, future.getId()-1);
			if(lastSubJobs.getDepartureTime()+tvjlastSubJobs<=future.getArrivalTime()) {// structure a---b---j
				preliminarySubJobList.clear();
				for(SubJobs inRoute:paramedic.getListSubJobs()) {
					preliminarySubJobList.add(inRoute);
				}
				//				Couple c= new Couple(dropoffpatientMedicalCentre.get(j.getSubJobKey()), inp,test);
				//				SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
				//				SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
				preliminarySubJobList.add(present);
				preliminarySubJobList.add(future);
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
					merge=true;
					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(future);
					paramedic.getDirectorySubjobs().put(present.getSubJobKey(), present);
					paramedic.getDirectorySubjobs().put(future.getSubJobKey(), future);
				}
			}
		}
		return merge;
	}

	private boolean insertingPairSubJobsDropOffClient(SubJobs j, Parts paramedic) {
		boolean merge=false;
		boolean inserted=false;
		ArrayList<SubJobs> preliminarySubJobList= new ArrayList<>();
		inserted=iteratingOverSequenceSubJobsDPclient(preliminarySubJobList,j,paramedic,merge); // structure a---j---b
		if(!inserted) { // insertarlo al final
			SubJobs lastSubJobs=paramedic.getListSubJobs().get(paramedic.getListSubJobs().size()-1);
			double tvjlastSubJobs=inp.getCarCost().getCost(lastSubJobs.getId()-1, j.getId()-1);
			if(lastSubJobs.getDepartureTime()+tvjlastSubJobs<=j.getArrivalTime()) {// structure a---b---j
				preliminarySubJobList.clear();
				for(SubJobs inRoute:paramedic.getListSubJobs()) {
					preliminarySubJobList.add(inRoute);
				}

				Couple c= new Couple(dropoffHomeCareStaff.get(j.getSubJobKey()), inp,test);
				SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
				preliminarySubJobList.add(present);
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
					merge=true;
					paramedic.getListSubJobs().add(present);
				}
			}
		}
		else {
			merge=true;
		}
		return merge;
	}

	private boolean insertingPairSubJobsPickUpClient(SubJobs present, Parts paramedic) {
		boolean merge=false;
		boolean inserted=false;
		ArrayList<SubJobs> preliminarySubJobList= new ArrayList<>();
		inserted=iteratingOverSequenceSubJobsPickUpclient(preliminarySubJobList,present,paramedic,merge); // structure a---j---b
		if(!inserted) { // insertarlo al final
			SubJobs lastSubJobs=paramedic.getListSubJobs().get(paramedic.getListSubJobs().size()-1);
			double tvjlastSubJobs=inp.getCarCost().getCost(lastSubJobs.getId()-1, present.getId()-1);
			if(lastSubJobs.getDepartureTime()+tvjlastSubJobs<=present.getArrivalTime()) {// structure a---b---j
				preliminarySubJobList.clear();
				for(SubJobs inRoute:paramedic.getListSubJobs()) {
					preliminarySubJobList.add(inRoute);
				}
				//				Couple c= new Couple(pickUpHomeCareStaff.get(present.getSubJobKey()), inp,test);
				//				SubJobs present=(SubJobs)c.getStartEndNodes().get(0);
				preliminarySubJobList.add(present);
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
					merge=true;
					paramedic.getListSubJobs().add(present);
				}
			}
		}
		else {
			merge=true;
		}
		return merge;
	}

	private boolean iteratingOverSequenceSubJobsDPclient(ArrayList<SubJobs> preliminarySubJobList, SubJobs jpresent, Parts paramedic,
			boolean merge) {
		// structure a---j---b
		if(jpresent.getSubJobKey().equals("D22")) {
			//System.out.println("stop");
		}
		boolean insertedSecondPart=false;
		//Couple c= new Couple(dropoffHomeCareStaff.get(jpresent.getSubJobKey()), inp,test);
		//SubJobs jpresent=(SubJobs)c.getStartEndNodes().get(1);
		double possibleArrival=jpresent.getArrivalTime();
		int position=-1;
		preliminarySubJobList.add(paramedic.getListSubJobs().get(0));
		for(int i=0;i<paramedic.getListSubJobs().size()-1;i++) {
			SubJobs a=paramedic.getListSubJobs().get(i);
			SubJobs b=paramedic.getListSubJobs().get(i+1);
			preliminarySubJobList.add(b);
			//if(!inserted) {
			double tvaj=inp.getCarCost().getCost(a.getId()-1, jpresent.getId()-1);
			double tvjb=inp.getCarCost().getCost(jpresent.getId()-1, b.getId()-1);

			if(a.getDepartureTime()+tvaj<=jpresent.getArrivalTime() && jpresent.getDepartureTime()+tvjb<=b.getArrivalTime()) {
				preliminarySubJobList.add(i+1,jpresent);
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(workingHoursDriver) {
					position=i+1;
					//paramedic.getListSubJobs().add(position,jpresent);
					insertedSecondPart=true;}
				break;	
			}
			else {
				possibleArrival=a.getDepartureTime()+tvaj;
				double possibleDeparture=possibleArrival+jpresent.getdeltaArrivalDeparture();
				if(possibleArrival>=a.getDepartureTime() && (possibleDeparture+tvjb)<= b.getArrivalTime()) {
					if(possibleArrival>=jpresent.getStartTime() && possibleArrival<= jpresent.getEndTime()) {
						SubJobs copy= new SubJobs(jpresent);
						position=i+1;
						copy.setarrivalTime(possibleArrival);
						copy.setStartServiceTime(possibleArrival+copy.getdeltaArrivalStartServiceTime());
						copy.setEndServiceTime(possibleArrival+copy.getdeltarStartServiceTimeEndServiceTime());
						copy.setdepartureTime(possibleArrival+copy.getdeltaArrivalDeparture());
						preliminarySubJobList.add(position,copy);
						boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
						if(workingHoursDriver) {
							insertedSecondPart=true;}
						break;	
					}
				}
			}
			//}
		}
		if(insertedSecondPart) { // se intenta insertar la otra parte
			boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
			if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
				jpresent.setarrivalTime(possibleArrival);
				jpresent.setStartServiceTime(possibleArrival+jpresent.getdeltaArrivalStartServiceTime());
				jpresent.setEndServiceTime(possibleArrival+jpresent.getdeltarStartServiceTimeEndServiceTime());
				jpresent.setdepartureTime(possibleArrival+jpresent.getdeltaArrivalDeparture());
				paramedic.getListSubJobs().add(position,jpresent);
				merge=true;
			}
		}
		return merge;
	}

	private boolean iteratingOverSequenceSubJobsPickUpclient(ArrayList<SubJobs> preliminarySubJobList, SubJobs jpresent, Parts paramedic,
			boolean merge) {
		// structure a---j---b
		if(jpresent.getSubJobKey().equals("P22")) {
			//System.out.println("Stop");
		}
		//Couple c1= pickUpHomeCareStaff.get(j.getSubJobKey());
		//		Couple c= new Couple(pickUpHomeCareStaff.get(j.getSubJobKey()), inp,test);
		//		SubJobs jpresent=(SubJobs)c.getStartEndNodes().get(0);
		double possibleArrival=jpresent.getArrivalTime();
		int position=-1;
		preliminarySubJobList.add(paramedic.getListSubJobs().get(0));
		for(int i=0;i<paramedic.getListSubJobs().size()-1;i++) {
			SubJobs a=paramedic.getListSubJobs().get(i);
			SubJobs b=paramedic.getListSubJobs().get(i+1);

			preliminarySubJobList.add(b);
			//if(!inserted) {
			double tvaj=inp.getCarCost().getCost(a.getId()-1, jpresent.getId()-1);
			double tvjb=inp.getCarCost().getCost(jpresent.getId()-1, b.getId()-1);

			if(a.getDepartureTime()+tvaj<=jpresent.getArrivalTime() && jpresent.getDepartureTime()+tvjb<=b.getArrivalTime()) {
				preliminarySubJobList.add(i+1,jpresent);
				position=i+1;
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
					merge=true;}
				break;	
			}
			else {
				possibleArrival=a.getDepartureTime()+tvaj;
				double possibleDeparture=possibleArrival+jpresent.getdeltaArrivalDeparture();
				if(possibleArrival>=a.getDepartureTime() && (possibleDeparture+tvjb)<= b.getArrivalTime()) {
					if(possibleArrival>=jpresent.getStartTime() && possibleArrival<= jpresent.getEndTime()) {
						SubJobs copy=new SubJobs(jpresent);
						copy.setarrivalTime(possibleArrival);
						copy.setStartServiceTime(possibleArrival+copy.getdeltaArrivalStartServiceTime());
						copy.setEndServiceTime(possibleArrival+copy.getdeltarStartServiceTimeEndServiceTime());
						copy.setdepartureTime(possibleArrival+copy.getdeltaArrivalDeparture());
						preliminarySubJobList.add(i+1,copy);
						position=i+1;
						boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
						if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
							merge=true;}
						break;	
					}
				}
			}
			//}
		}
		if(merge) { // se intenta insertar la otra parte
			if(vehicleCapacityPart(preliminarySubJobList)) {
				jpresent.setarrivalTime(possibleArrival);
				jpresent.setStartServiceTime(possibleArrival+jpresent.getdeltaArrivalStartServiceTime());
				jpresent.setEndServiceTime(possibleArrival+jpresent.getdeltarStartServiceTimeEndServiceTime());
				jpresent.setdepartureTime(possibleArrival+jpresent.getdeltaArrivalDeparture());
				paramedic.getListSubJobs().add(position,jpresent);

			}
		}
		return merge;
	}


	private boolean iteratingOverSequenceSubJobsDP(ArrayList<SubJobs> preliminarySubJobList, SubJobs present, SubJobs future, Parts paramedic,
			boolean merge) {
		// structure a---j---b
		boolean inserted=false;
		int position=-1;
		preliminarySubJobList.add(paramedic.getListSubJobs().get(0));
		for(int i=0;i<paramedic.getListSubJobs().size()-1;i++) {
			SubJobs a=paramedic.getListSubJobs().get(i);
			SubJobs b=paramedic.getListSubJobs().get(i+1);
			//preliminarySubJobList.add(a);
			preliminarySubJobList.add(b);
			double tvaj=inp.getCarCost().getCost(a.getId()-1, future.getId()-1);
			double tvjb=inp.getCarCost().getCost(future.getId()-1, b.getId()-1);

			if(a.getDepartureTime()+tvaj<=future.getArrivalTime() && future.getDepartureTime()+tvjb<=b.getArrivalTime()) {


				preliminarySubJobList.add(i+1,future);
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(workingHoursDriver) {
					position=i+1;
					inserted=true;}
				break;

			}
		}
		if(inserted) { // se intenta insertar la otra parte
			SubJobs pickUpHome=new SubJobs(dropoffpatientMedicalCentre.get(future.getSubJobKey()).getStartEndNodes().get(1));
			for(int i=0;i<preliminarySubJobList.size()-1;i++) {
				SubJobs a=paramedic.getListSubJobs().get(i);
				SubJobs b=paramedic.getListSubJobs().get(i+1);
				double tvapickUpHome=inp.getCarCost().getCost(a.getId()-1, pickUpHome.getId()-1);
				double tvpickUpHomeb=inp.getCarCost().getCost(pickUpHome.getId()-1, b.getId()-1);
				if(a.getDepartureTime()+tvapickUpHome<=pickUpHome.getArrivalTime()) {
					if(pickUpHome.getDepartureTime()+tvpickUpHomeb<=b.getArrivalTime()) {
						preliminarySubJobList.add(i+1,future);
						boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
						if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
							merge=true;
							//							Couple c= new Couple(dropoffpatientMedicalCentre.get(j.getSubJobKey()), inp,test);
							//							SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
							//SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
							paramedic.getListSubJobs().add(position,future);
							paramedic.getListSubJobs().add(i+1,present);
							paramedic.getDirectorySubjobs().put(present.getSubJobKey(), present);
							paramedic.getDirectorySubjobs().put(future.getSubJobKey(), future);
							break;
						}
						else {
							preliminarySubJobList.remove(i+1);
						}

					}	
				}
			}

		}
		return merge;
	}

	private boolean checkingWorkingDriver(ArrayList<SubJobs> preliminarySubJobList) {
		boolean workingHoras=false;
		double tvfromDepot=inp.getCarCost().getCost(0, preliminarySubJobList.get(0).getId()-1);
		double tvtoDepot=inp.getCarCost().getCost(preliminarySubJobList.get(preliminarySubJobList.size()-1).getId()-1, 0);
		double routeLenght=preliminarySubJobList.get(preliminarySubJobList.size()-1).getDepartureTime()-preliminarySubJobList.get(0).getArrivalTime();
		if(routeLenght+tvfromDepot+tvtoDepot<test.getRouteLenght()) {
			workingHoras=true;
		}
		if(!workingHoras) {
			//System.out.println("Stop");
		}			
		return workingHoras;
	}

	private boolean insertingPairSubJobsPickUpDropOffPatient(SubJobs j, Parts paramedic) {
		boolean merge=false;
		if(j.getSubJobKey().equals("P4759")) {
			//System.out.println("Stop");
		}
		boolean inserted=false;
		ArrayList<SubJobs> preliminarySubJobList= new ArrayList<>();
		inserted=iteratingOverSequenceSubJobsPD(preliminarySubJobList,j,paramedic,merge); // structure a---j---b

		if(!inserted) { // insertarlo al final
			SubJobs lastSubJobs= new SubJobs(paramedic.getListSubJobs().get(paramedic.getListSubJobs().size()-1));
			double tvjlastSubJobs=inp.getCarCost().getCost(lastSubJobs.getId()-1, j.getId()-1);
			if(lastSubJobs.getDepartureTime()+tvjlastSubJobs<=j.getArrivalTime()) {// structure a---b---j
				preliminarySubJobList.clear();
				for(SubJobs inRoute:paramedic.getListSubJobs()) {
					preliminarySubJobList.add(inRoute);
				}
				Couple c= new Couple(pickpatientMedicalCentre.get(j.getSubJobKey()), inp,test);
				SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
				SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
				preliminarySubJobList.add(present);
				preliminarySubJobList.add(future);
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
					merge=true;
					// changing time


					// adding
					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(future);
				}
			}
		}
		else {
			merge=true;
		}
		return merge;
	}

	private boolean iteratingOverSequenceSubJobsPD(ArrayList<SubJobs> preliminarySubJobList, SubJobs j, Parts paramedic,
			boolean merge) {
		// structure a---j---b
		boolean inserted=false;
		Couple c= new Couple(pickpatientMedicalCentre.get(j.getSubJobKey()), inp,test);
		SubJobs jpresent=(SubJobs)c.getStartEndNodes().get(1);
		SubJobs jfuture=(SubJobs)c.getStartEndNodes().get(0);
		double possibleArrival=jpresent.getArrivalTime();
		int position=-1;
		preliminarySubJobList.add(paramedic.getListSubJobs().get(0));
		for(int i=0;i<paramedic.getListSubJobs().size()-1;i++) {
			SubJobs a=paramedic.getListSubJobs().get(i);
			SubJobs b=paramedic.getListSubJobs().get(i+1);

			preliminarySubJobList.add(b);

			double tvaj=inp.getCarCost().getCost(a.getId()-1, jpresent.getId()-1);
			double tvjb=inp.getCarCost().getCost(jpresent.getId()-1, b.getId()-1);

			if(a.getDepartureTime()+tvaj<=jpresent.getArrivalTime() && jpresent.getDepartureTime()+tvjb<=b.getArrivalTime()) {
				preliminarySubJobList.add(i+1,j);
				boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
				if(workingHoursDriver) {
					position=i+1;
					inserted=true;}
				break;	
			}
			else {
				possibleArrival=a.getDepartureTime()+tvaj;
				double possibleDeparture=possibleArrival+jpresent.getdeltaArrivalDeparture();
				if(possibleArrival>=a.getDepartureTime() && (possibleDeparture+tvjb)<= b.getArrivalTime()) {
					if(possibleArrival>=jpresent.getStartTime() && possibleArrival<= jpresent.getEndTime()) {
						SubJobs copy=new SubJobs(j);
						copy.setarrivalTime(possibleArrival);
						copy.setStartServiceTime(possibleArrival+copy.getdeltaArrivalStartServiceTime());
						copy.setEndServiceTime(possibleArrival+copy.getdeltarStartServiceTimeEndServiceTime());
						copy.setdepartureTime(possibleArrival+copy.getdeltaArrivalDeparture());
						preliminarySubJobList.add(i+1,copy);
						boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
						if(workingHoursDriver) {
							position=i+1;
							inserted=true;}
						break;	
					}
				}
			}
		}
		if(inserted) { // se intenta insertar la otra parte
			boolean secondPart=false;
			double deltaArrival=jfuture.getArrivalTime()-jpresent.getArrivalTime();

			double possibleArrivalDropOff=possibleArrival+deltaArrival;
			double possibleDeparture=possibleArrivalDropOff+jfuture.getdeltaArrivalDeparture();

			for(int i=position;i<preliminarySubJobList.size()-1;i++) {
				SubJobs a=preliminarySubJobList.get(i);
				SubJobs b=preliminarySubJobList.get(i+1);
				double tvapickUpHome=inp.getCarCost().getCost(a.getId()-1, jfuture.getId()-1);
				double tvpickUpHomeb=inp.getCarCost().getCost(jfuture.getId()-1, b.getId()-1);
				if(a.getDepartureTime()+tvapickUpHome<=possibleArrivalDropOff) {
					if((possibleDeparture+tvpickUpHomeb)<=b.getArrivalTime()) {
						preliminarySubJobList.add(i+1,jfuture);
						boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);


						if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
							merge=true;
							secondPart=true;

							jpresent.setarrivalTime(possibleArrival);
							jpresent.setStartServiceTime(possibleArrival+jpresent.getdeltaArrivalStartServiceTime());
							jpresent.setEndServiceTime(possibleArrival+jpresent.getdeltarStartServiceTimeEndServiceTime());
							jpresent.setdepartureTime(possibleArrival+jpresent.getdeltaArrivalDeparture());

							jfuture.setarrivalTime(possibleArrivalDropOff);
							jfuture.setStartServiceTime(possibleArrivalDropOff+jpresent.getdeltaArrivalStartServiceTime());
							jfuture.setEndServiceTime(possibleArrivalDropOff+jpresent.getdeltarStartServiceTimeEndServiceTime());
							jfuture.setdepartureTime(possibleArrivalDropOff+jpresent.getdeltaArrivalDeparture());
							paramedic.getListSubJobs().add(position,jpresent);
							paramedic.getListSubJobs().add(i+1,jfuture);
							break;
						}
						else {
							preliminarySubJobList.remove(i+1);
						}

					}	
				}
			}
			if(!secondPart) {
				SubJobs lastSubJobs=paramedic.getListSubJobs().get(paramedic.getListSubJobs().size()-1);
				double tvjlastSubJobs=inp.getCarCost().getCost(lastSubJobs.getId()-1, j.getId()-1);
				if(lastSubJobs.getDepartureTime()+tvjlastSubJobs<=jfuture.getArrivalTime()) {// structure a---b---j
					preliminarySubJobList.clear();
					for(SubJobs inRoute:paramedic.getListSubJobs()) {
						preliminarySubJobList.add(inRoute);
					}
					preliminarySubJobList.add(jpresent);
					preliminarySubJobList.add(jfuture);
					boolean workingHoursDriver=checkingWorkingDriver(preliminarySubJobList);
					if(vehicleCapacityPart(preliminarySubJobList) && workingHoursDriver) {
						merge=true;
						jpresent.setarrivalTime(possibleArrival);
						jpresent.setStartServiceTime(possibleArrival+jpresent.getdeltaArrivalStartServiceTime());
						jpresent.setEndServiceTime(possibleArrival+jpresent.getdeltarStartServiceTimeEndServiceTime());
						jpresent.setdepartureTime(possibleArrival+jpresent.getdeltaArrivalDeparture());
						paramedic.getListSubJobs().add(position,jpresent);
						paramedic.getListSubJobs().add(jfuture);
					}
				}
			}
		}
		return merge;
	}

	private void assigmentJobsToQualifications(ArrayList<ArrayList<Couple>> clasification) { 
		// solo la asignación de trabajos a los niveles de calificaciones // se hace una secuencia ordenad_
		//  no se consideran los downgradings solo se asignan tareas de acuerdo con la compatibilidad de
		// de las ventanas de tiempo
		// 1. Clasification de trabajos de acuerdo a las qualificaciones
		//1. list of jobs
		// Classifying clients: Home care staff
		ArrayList<Jobs> clasification3 = creationJobsHomeCareStaff(clasification.get(0)); 
		clasification3.sort(Jobs.SORT_BY_STARTW);

		ArrayList<Jobs> clasification2 = creationJobsHomeCareStaff(clasification.get(1));
		clasification2.sort(Jobs.SORT_BY_STARTW);
		ArrayList<Jobs> clasification1 = creationJobsHomeCareStaff(clasification.get(2));
		clasification1.sort(Jobs.SORT_BY_STARTW);
		// Classifying patients: Paramedics
		ArrayList<Jobs> clasification0 = creationJobsParamedics(clasification.get(3));
		clasification0.sort(Jobs.SORT_BY_STARTW);
		// 2. Calling the type and quantity of home care staff
		List<AttributeNurse> homeCareStaff= inp.gethomeCareStaffInf(); // home Care Staff according the qualification level
		List<AttributeParamedics> paramedic= inp.getParamedic(); // paramedic qualification level
		// Home care staff for:
		int q3= homeCareStaff.get(2).getQuantity(); // Qualification 3
		int q2= homeCareStaff.get(1).getQuantity(); // Qualification 2
		int q1= homeCareStaff.get(0).getQuantity(); // Qualification 1
		int q0= paramedic.get(0).getQuantity(); // Qualification 0

		// 3. Definition of a feasible sequence of jobs for each qualification level
		//	ArrayList<ArrayList<Jobs>> qualification3= assigmentHighQualification(q3,clasification3);

		//Qualification level =0
		ArrayList<Parts> qualification0= assigmentParamedic(q0,clasification0);
		missingAssigment(qualification0,clasification0);
		//		//Qualification level from 1 to 3
		ArrayList<Parts> qualification1= assigmentParamedic(q1,clasification1); // here are not considering working hours
		missingAssigment(qualification1,clasification1);
		ArrayList<Parts> qualification2= assigmentParamedic(q2,clasification2);
		missingAssigment(qualification2,clasification2);
		ArrayList<Parts> qualification3= assigmentParamedic(q3,clasification3);
		missingAssigment(qualification3,clasification3);

		downgradings(qualification3,qualification2);
		downgradings(qualification2,qualification1); //No se considera porque no es tan facil controlar el tiempo de trabajo

		for(Parts schifts:qualification0) {
			Parts newParts= null;
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}

		// home care staff
		for(Parts schifts:qualification1) {
			Parts newParts= null;
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}



		for(Parts schifts:qualification2) {
			Parts newParts= null;
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}
		for(Parts schifts:qualification3) {
			Parts newParts= null;
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}
		//System.out.println("all turns");
		int i=-1;
		for(Parts s:schift) {
			i++;
			//System.out.println("turn "+i);
			printing(s);
		}

		//System.out.println("all turns");

	}



	private void missingAssigment(ArrayList<Parts> qualification12, ArrayList<Jobs> clasification1) {
		HashMap<Integer,Jobs> copy= new HashMap<Integer,Jobs>();
		for(Jobs s: clasification1) {
			copy.put(s.getId(),s);
		}
		clasification1.clear();
		for(Parts p: qualification12) {
			for(Jobs s: p.getListSubJobs()) {
				if(copy.containsKey(s.getId())) {
					copy.remove(s.getId());
				}
			}
		}

		for(Jobs s: copy.values()) {
			clasification1.add(s);
		}
		//System.out.println("all turns");
	}

	private void downgradings(ArrayList<Parts> highQualification, ArrayList<Parts> lowQualification) {
		// cada parte es un personal
		boolean insertion=false;
		for(Parts high:highQualification) {
			insertion=downgradingsPart(high,lowQualification);

		}	
		//System.out.println("Print solution"+insertion );
	}











	private boolean downgradingsPart(Parts high, ArrayList<Parts> lowQualification) {
		boolean insertion =false;
		// structure (firstHight)---(lastHight)---(firstLow)---(lastLow)
		for(Parts low:lowQualification) {
			if(!low.getListSubJobs().isEmpty() && !high.getListSubJobs().isEmpty()) {
				SubJobs firstHight=high.getListSubJobs().get(0); // head of the high cualification level
				SubJobs lastHight=high.getListSubJobs().get(high.getListSubJobs().size()-1); // head of the high cualification level
				SubJobs firstLow=low.getListSubJobs().get(0); // head of the low cualification level
				SubJobs lastLow=low.getListSubJobs().get(low.getListSubJobs().size()-1); // head of the low cualification level
				double distanceFromDepot=inp.getCarCost().getCost(0, firstHight.getId()-1);
				double distanceToDepot=inp.getCarCost().getCost(lastLow.getId()-1, 0);
				double distanceConnection=inp.getCarCost().getCost(lastHight.getId()-1, firstLow.getId()-1);
				if ((lastHight.getDepartureTime()-lastLow.getDepartureTime()+distanceFromDepot+distanceToDepot+distanceConnection)<test.getWorkingTime()) { // revisar las working horas
					insertion=tryingToInsert(distanceFromDepot,distanceToDepot,distanceConnection,firstLow,lastLow,lastLow,high,low);
				}
				else {
					insertion=tryingToInsertearly(high,low);
					if(!insertion) {
						insertion=tryingToInsertchangingTime(high,low);}
				}
				if(insertion) {
					insertion=false;
					low.getListSubJobs().clear();
				}
			}
		}


		return insertion;
	}

	private boolean tryingToInsertearly(Parts high, Parts low) {
		// structure (low) - (high)
		boolean inserted=false;
		SubJobs firstHight=high.getListSubJobs().get(0); // head of the high cualification level
		SubJobs lastHight=high.getListSubJobs().get(high.getListSubJobs().size()-1); // head of the high cualification level
		SubJobs firstLow=low.getListSubJobs().get(0); // head of the low cualification level
		SubJobs lastLow=low.getListSubJobs().get(low.getListSubJobs().size()-1); // head of the low cualification level
		double distanceFromDepot=inp.getCarCost().getCost(0, firstLow.getId()-1);
		double distanceToDepot=inp.getCarCost().getCost(lastHight.getId()-1, 0);
		double distanceConnection=inp.getCarCost().getCost(lastLow.getId()-1, firstHight.getId()-1);
		double possibleDepartureTime=firstHight.getArrivalTime()-distanceConnection;
		double workTime=lastHight.getDepartureTime()-firstLow.getArrivalTime()+distanceFromDepot+distanceToDepot+distanceConnection;
		if(workTime<test.getWorkingTime()) { // working time
			if(lastLow.getDepartureTime()<=possibleDepartureTime && lastLow.getDepartureTime()<firstHight.getArrivalTime()) {
				inserted=true;
				ArrayList<SubJobs> list= new ArrayList<SubJobs>();
				for(SubJobs j:low.getListSubJobs()) {
					list.add(j);
				}
				for(SubJobs j:high.getListSubJobs()) {
					list.add(j);
				}
				high.getListSubJobs().clear();
				for(SubJobs j:list) {
					high.getListSubJobs().add(j);
				}
			}
		}
		return inserted;
	}

	private boolean tryingToInsertchangingTime(Parts high, Parts low) {
		// quien cambia la hora son los trabajos que estan en low
		boolean inserted=false;

		SubJobs firstHight=high.getListSubJobs().get(0); // head of the high cualification level
		SubJobs lastHight=high.getListSubJobs().get(high.getListSubJobs().size()-1); // head of the high cualification level
		SubJobs firstLow=low.getListSubJobs().get(0); // head of the low cualification level
		SubJobs lastLow=low.getListSubJobs().get(low.getListSubJobs().size()-1); // head of the low cualification level
		if(lastHight.getDepartureTime()<firstLow.getStartTime()) {
			double distanceFromDepot=inp.getCarCost().getCost(0, firstHight.getId()-1);
			double distanceToDepot=inp.getCarCost().getCost(lastLow.getId()-1, 0);
			double distanceConnection=inp.getCarCost().getCost(lastHight.getId()-1, firstLow.getId()-1);
			// possible new times
			double arrivalTime=lastHight.getDepartureTime()+distanceConnection;
			double possibleStartTime=arrivalTime+test.getloadTimeHomeCareStaff();
			if(possibleStartTime>=firstLow.getStartTime() && possibleStartTime<=lastLow.getEndTime()) {
				ArrayList<SubJobs> part=calculating(possibleStartTime,low);
				if(!part.isEmpty()) {
					double newEndLow=part.get(part.size()-1).getDepartureTime();
					if((newEndLow-firstHight.getArrivalTime()+distanceFromDepot+distanceToDepot+distanceConnection)<=test.getWorkingTime()) {
						inserted=true;
						changingTimeLow(part,low);
					}
				}
			}
		}
		return inserted;
	}

	private void changingTimeLow(ArrayList<SubJobs> part, Parts low) {
		for(int i=0;i<part.size(); i++) {
			low.getListSubJobs().get(i).setarrivalTime(part.get(i).getArrivalTime());
			low.getListSubJobs().get(i).setStartServiceTime(part.get(i).getstartServiceTime());
			low.getListSubJobs().get(i).setEndServiceTime(part.get(i).getendServiceTime());
			low.getListSubJobs().get(i).setdepartureTime(part.get(i).getDepartureTime());
		}

	}

	private ArrayList<SubJobs> calculating(double possibleStartTime, Parts lowRef) {
		double newEnd=0;
		ArrayList<SubJobs> low= new ArrayList<SubJobs>();
		for(SubJobs j:lowRef.getListSubJobs()) {
			low.add(new SubJobs(j));
		}
		double travelTime=0;
		double serviceTime=0;
		double arrivalTime=possibleStartTime;
		double startServiceTime=0;
		double endServiceTime=0;
		double departureTime=0;

		for(int i=0;i<low.size()-1;i++) {

			travelTime=inp.getCarCost().getCost(low.get(i).getId()-1, low.get(i+1).getId()-1);
			low.get(i).setarrivalTime(possibleStartTime-test.getloadTimeHomeCareStaff());
			low.get(i).setStartServiceTime(possibleStartTime);
			low.get(i).setEndServiceTime(possibleStartTime+low.get(i).getReqTime());
			low.get(i).setdepartureTime(low.get(i).getendServiceTime()+test.getloadTimeHomeCareStaff());
			possibleStartTime=low.get(i).getDepartureTime()+travelTime;
		}
		return low;
	}

	private boolean tryingToInsert(double distanceFromDepot, double distanceToDepot, double distanceConnection,
			SubJobs lastHight, SubJobs firstLow, SubJobs lastLow, Parts high, Parts low) {
		boolean inserted=false;
		if((lastHight.getDepartureTime()+distanceConnection) <=firstLow.getstartServiceTime() && lastHight.getDepartureTime()>firstLow.getArrivalTime()) { // revisar la ventana de tiempo

			inserted=true;
			//	setListSubJobs(ArrayList<SubJobs> listSubJobs, Inputs inp, Test test)
			ArrayList<SubJobs> newListJob= new ArrayList<SubJobs>();
			for(SubJobs p:high.getListSubJobs()) {
				newListJob.add(p);
			}
			for(SubJobs p:low.getListSubJobs()) {
				newListJob.add(p);
			}
			high.getListSubJobs().clear();
			high.setListSubJobs(newListJob, inp, test);
		}
		else {

			inserted=tryingToInsertearly(high,low);
			if(!inserted) {
				inserted=tryingToInsertchangingTime(high,low);}
		}
		return inserted;
	}

	private boolean tryToInsertInHigherShift(ArrayList<SubJobs> shift, ArrayList<ArrayList<SubJobs>> qualification22) {
		// check if it can be inserted after or before
		boolean inserted= false;	
		int i=-1;
		for(ArrayList<SubJobs> a:qualification22) { // reading over the high qualification level
			if(!a.isEmpty()) {
				i++;
				inserted= keepingTimes(a,shift,qualification22,i);
				if(!inserted) {
					break;
				}	
			}
		}
		return inserted;
	}


	private boolean keepingTimes(ArrayList<SubJobs> a, ArrayList<SubJobs> shift, ArrayList<ArrayList<SubJobs>> qualification22, int i) {
		boolean inserted= false;	
		// times of the high qualification level
		double startTimeHigherQualification=a.get(0).getArrivalTime();
		int indexLastJob=a.size();
		double endTimeHigherQualification=a.get(indexLastJob-1).getArrivalTime();
		// times of the low qualification level
		double startTimelowerQualification=shift.get(0).getArrivalTime();
		indexLastJob=shift.size();
		double endTimelowerQualification=shift.get(indexLastJob-1).getArrivalTime();
		if(startTimeHigherQualification<startTimelowerQualification &&  endTimeHigherQualification<endTimelowerQualification ) {// (higher qualifications)-------- (lower qualification)
			qualification22.add(shift);
			inserted=true;
		}
		else {// (lower qualifications)-------- (higher qualification)
			if(startTimeHigherQualification>startTimelowerQualification &&  endTimeHigherQualification>endTimelowerQualification ) {
				qualification22.add(i-1,shift);
				inserted=true;
			}
		}
		return inserted;
	}

	private ArrayList<ArrayList<SubJobs>> copyArrayOfArrays(ArrayList<ArrayList<SubJobs>> qualification2) {
		ArrayList<ArrayList<SubJobs>> copy= new ArrayList<ArrayList<SubJobs>> (); /// solo se copian los elementos que no estan vacios
		for(ArrayList<SubJobs> a:qualification2) {
			if(!a.isEmpty()) {
				copy.add(a);
			}
		}
		return copy;
	}

	private ArrayList<Route> insertingDepotConnections(ArrayList<Parts> schift2) {
		// 1. each turn is a route
		ArrayList<Route> route= new ArrayList<Route>();
		for(Parts turn:schift2 ) { // cada turno se convertira en una ruta
			if(!turn.getListSubJobs().isEmpty()) {
				makeTurnInRoute(turn,route);
			}
		}
		// 2. compute the the start and end time of route
		timeStartEndRoutes(route);
		//3. compute the connections between SubJobs
		settingEdges(route);
		creatingSchifts(route);

		return route;
	}

	private void creatingSchifts(ArrayList<Route> route) {
		int intRoute=0;
		for(Route r:route) {
			intRoute++;
			Schift personnal= new Schift(r,intRoute);
			r.setSchiftRoute(personnal);
		}
	}

	private void settingEdges(ArrayList<Route> route) {
		// concection per parts	
		for(Route r:route) {

			for(Parts p: r.getPartsRoute()) {
				for(int i=1;i<p.getListSubJobs().size()-1;i++) {
					SubJobs previous=r.getSubJobsList().get(i-1);
					SubJobs job=r.getSubJobsList().get(i);
					Edge e=new Edge(previous,job,inp,test);
					p.getDirectoryConnections().put(e.getEdgeKey(), e);
				}
			}
		}
		//1.  set the connection per route

		for(Route r:route) {		// 1. copy the list of elements into the route
			ArrayList<SubJobs> sequence= new ArrayList<SubJobs>(); // storing the jobs in the route
			r.getEdges().clear();
			for(Parts p:r.getPartsRoute()) {
				for(SubJobs sj:p.getListSubJobs()) {
					sequence.add(sj);
				}	
			}

			for(int i=1;i<sequence.size()-1;i++) {
				SubJobs origen=sequence.get(i-1);
				SubJobs end=sequence.get(i);
				Edge ef= null;
				if(origen.getId()!=end.getId()) {
					ef=new Edge(origen,end,inp,test);
					r.getEdges().put(ef.getEdgeKey(), ef);
				}

			}
		}
	}

	private void timeStartEndRoutes(ArrayList<Route> route) {
		// 1. computing times Route
		for(Route r:route) {
			//1. start time
			if(r.getSubJobsList().size()==0) {
				//System.out.println("Stop");		
			}
			SubJobs firstJob=r.getSubJobsList().get(0); // it is not the depot node

			computeStartTimeRoute(firstJob,r);
			//r.getSubJobsList().get(0).setserviceTime(0);
			// end time
			SubJobs lastJob=r.getSubJobsList().get(r.getSubJobsList().size()-1);  // it is not the depot node
			computeEndTimeRoute(lastJob,r);
			//r.getSubJobsList().get(r.getSubJobsList().size()-1).setserviceTime(0);
		}
	}

	private void computeEndTimeRoute(SubJobs lastJob, Route r) {
		// 1. Compute travel time
		SubJobs depot=r.getPartsRoute().get(r.getPartsRoute().size()-1).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(lastJob.getId()-1, depot.getId()-1);
		double arrivalTime=	lastJob.getDepartureTime()+tv;
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
		depot.setserviceTime(0);
		depot.setStartTime(0);
		depot.setEndTime(0);
		//System.out.println(r.toString());
	}

	private void computeStartTimeRoute(SubJobs firstJob, Route r) {
		// 1. Compute travel time
		SubJobs depot=r.getPartsRoute().get(0).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(depot.getId()-1,firstJob.getId()-1);
		double arrivalTime=	firstJob.getArrivalTime()-tv;
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
		depot.setserviceTime(0);
		depot.setStartTime(0);
		depot.setEndTime(0);
		//System.out.println(r.toString());
	}

	private void makeTurnInRoute(Parts turn, ArrayList<Route> route) { // crea tantas rutas como son posibles
		// la creación de estas rutas lo que hace es identificar las partes de cada turno
		// pero en esencia sólo deberia agregar el deport

		//System.out.println(turn.toString());
		//	calling depot
		//Jobs depot=inp.getNodes().get(0);
		SubJobs depotStart = new SubJobs(inp.getNodes().get(0));
		depotStart.setTotalPeople(1);
		SubJobs depotEnd = new SubJobs(inp.getNodes().get(0));
		depotEnd.setTotalPeople(-1);
		ArrayList<SubJobs> partStart= new ArrayList<SubJobs>();
		ArrayList<SubJobs> partEnd= new ArrayList<SubJobs>();
		partStart.add(depotStart);

		partEnd.add(depotEnd);
		Parts partObject= new Parts();
		Route r= new Route();
		route.add(r);
		partObject.setListSubJobs(partStart,inp,test);
		r.getPartsRoute().add(partObject);

		// 1. hacer las partes
		double passengers=1;
		ArrayList<SubJobs> part= new ArrayList<SubJobs>();
		part= new ArrayList<SubJobs>();
		partObject= new Parts();
		r.getPartsRoute().add(partObject);
		r.updateRouteFromSubJobs(inp, test, jobsInWalkingRoute,partStart,partEnd,turn.getListSubJobs());

		//		for(int i=0;i<turn.getListSubJobs().size();i++) {
		//			SubJobs sj=turn.getListSubJobs().get(i);
		//			passengers+=sj.getTotalPeople();
		//			if(passengers!=0) {
		//				partObject.getListSubJobs().add(sj);
		//				if(i==turn.getListSubJobs().size()-1) {
		//					r.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		//					//System.out.println(r.toString());
		//				}
		//			}
		//			else {
		//				partObject.getListSubJobs().add(sj);
		//				partObject= new Parts();
		//				r.getPartsRoute().add(partObject);
		//				//System.out.println(r.toString());
		//			}		
		//		}
		//		partObject= new Parts();
		//		partObject.setListSubJobs(partEnd,inp,test);
		//		r.getPartsRoute().add(partObject);
		//		r.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		// setting personnal
		if(turn.getListSubJobs().get(0).isPatient()) {
			r.setAmountDriver(0);
			r.setAmountParamedic(1);
			r.setHomeCareStaff(0);
		}
		else {
			r.setAmountDriver(0);
			r.setAmountParamedic(0);
			r.setHomeCareStaff(1);
		}
		//System.out.println("Route");
		//System.out.println(r.toString());
		//System.out.println("end");
	}

	private void changingArrivalTimeSequence(Route r) {
		for(Jobs j:r.getSubJobsList()) {
			double loadTime=0;
			if(j.isClient()) {
				loadTime=test.getloadTimeHomeCareStaff();
			}
			else {
				loadTime=test.getloadTimePatient();
			}
			j.setarrivalTime(j.getstartServiceTime()-loadTime);
			j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
		}
	}


	private void settingDropOff(Jobs jobsInRoute,Jobs dropOff) {
		dropOff.setTotalPeople(-1);
		double tv=inp.getCarCost().getCost(jobsInRoute.getId()-1, dropOff.getId()-1);
		//double tv=inp.getCarCost().getCost(jobsInRoute.getId()-1, dropOff.getId()-1)*test.getDetour();
		// Time window
		double earlyTime=0;
		double laterTime=0;
		if(dropOff.isPatient() || dropOff.isMedicalCentre()) {
			earlyTime=jobsInRoute.getstartServiceTime()+tv+test.getloadTimePatient();
			laterTime=earlyTime+test.getCumulativeWaitingTime()+test.getloadTimePatient();
		}
		else {
			earlyTime=jobsInRoute.getstartServiceTime()+tv+test.getloadTimeHomeCareStaff();
			laterTime=earlyTime+test.getCumulativeWaitingTime()+test.getloadTimeHomeCareStaff();
		}
		dropOff.setStartTime(earlyTime);
		dropOff.setEndTime(laterTime);
		dropOff.setStartServiceTime(laterTime);
		dropOff.setserviceTime(0);
	}

	private void settingPickUp(Jobs pickUp) {
		// 3. how many people involve the service // time window // preliminary service start time
		pickUp.setTotalPeople(2);
		// Time window
		double earlyTime=pickUp.getendServiceTime();
		double laterTime=earlyTime+test.getCumulativeWaitingTime();
		pickUp.setStartTime(earlyTime);
		pickUp.setEndTime(laterTime);
		pickUp.setStartServiceTime(laterTime);
		pickUp.setserviceTime(0);
	}

	private ArrayList<Route> copyListRoute(Solution s) {
		ArrayList<Route> copyrouteList= new ArrayList<Route>();
		for(Route r:s.getRoutes()) {
			Route newCopy=copyRoute(r);	
			copyrouteList.add(newCopy);
		}
		return copyrouteList;
	}

	private Route copyRoute(Route r) {
		Route newCopy=new Route (r);	
		return newCopy;
	}


	private Jobs extractingJobInformationClient(Route r, Jobs jobsInRoute) {
		Jobs pair=null;
		// 1. calling job
		if(jobsInRoute.isClient()) { // setting the pick up of the home care staff
			pair= callingPairClient(jobsInRoute); // 2. calling tasks
		}
		else {
			if(jobsInRoute.isMedicalCentre() ) {  // setting the pick up of paramedic and patient
				pair= callingPairPatient(jobsInRoute); // 2. calling tasks
				settingInformationPair(pair, jobsInRoute); // 3. how many people involve the service // time window // preliminary service start time
			}
		}
		return pair;
	}

	private void settingInformationPair(Jobs pair, Jobs inRoute) {
		// 3. how many people involve the service // time window // preliminary service start time
		pair.setTotalPeople(-1);
		// Time window
		double tv=inp.getCarCost().getCost(inRoute.getId()-1, pair.getId()-1);
		//double tv=inp.getCarCost().getCost(inRoute.getId()-1, pair.getId()-1)*test.getDetour();
		double earlyTime=inRoute.getendServiceTime()+tv;
		double laterTime=earlyTime+test.getCumulativeWaitingTime();
		pair.setStartTime(earlyTime);
		pair.setEndTime(laterTime);
		pair.setStartServiceTime(laterTime);
	}


	private Jobs callingPairClient(Jobs jobsInRoute) {
		Jobs pair=null;
		if(jobsInRoute.getsubJobPair()!=null) {
			pair=jobsInRoute.getsubJobPair();
		}
		return pair;
	}

	private Jobs callingPairPatient(Jobs jobsInRoute) {
		Jobs pair=new Jobs(inp.getNodes().get(jobsInRoute.getIdUser()-1));	
		return pair;
	}




















	private ArrayList<Jobs> creationJobsHomeCareStaff(ArrayList<Couple> qualification) {
		ArrayList<Jobs> clasification = new ArrayList<Jobs>();
		HashMap<String, Jobs> list= new HashMap<String, Jobs> ();
		// home care Staff
		for(Couple c:qualification) {
			if(c.getPresent().getTotalPeople()<0) {
				list.put(c.getPresent().getSubJobKey(),c.getPresent());
			}

		}

		for(Jobs j:list.values()) {
			clasification.add(j);
		}
		clasification.sort(Jobs.SORT_BY_STARTW);
		return clasification;

	}


	private ArrayList<Jobs> creationJobsParamedics(ArrayList<Couple> qualification) {
		ArrayList<Jobs> clasification = new ArrayList<Jobs>();
		HashMap<String, Jobs> list= new HashMap<String, Jobs> ();
		// paramedic
		for(Couple c:qualification) {
			if(c.getFuture().isMedicalCentre() && c.getFuture().getTotalPeople()<0 ) {
				if(!assignedJobs.containsKey(c.getFuture().getId())) {
					list.put(c.getFuture().getSubJobKey(),c.getFuture());
				}
			}
		}
		for(Jobs j:list.values()) {
			clasification.add(j);
		}
		clasification.sort(Jobs.SORT_BY_STARTW);
		return clasification;

	}




	private ArrayList<Parts> assigmentParamedic(int q3, ArrayList<Jobs> clasification3) { // return the list of subJobs
		double qualification=-1;
		qualificationParamedic= new ArrayList<> (q3);
		for(int i=0;i<q3;i++) {
			Parts newPart=new Parts();
			qualificationParamedic.add(newPart);
		}
		// se guarda los schift
		for(Jobs taskToInert:clasification3) { // iterate over jobs
			Jobs j= new Jobs(taskToInert);
			for(Parts paramedic:qualificationParamedic) {
				if(paramedic.getDirectorySubjobs().containsKey("D24") &&  j.getId()==14) {
					//System.out.println("stop");
				}
				if(paramedic.getDirectorySubjobs().containsKey("D14") &&  j.getId()==9) {
					//System.out.println("stop");
				}
				if(j.getId()==30) {
					//System.out.println(" Turn ");
					//System.out.println(j.toString());
				}
				if(j.getId()==9) {
					//System.out.println(" Turn ");
					//System.out.println(j.toString());
				}
				if(j.getId()==15) {
					//System.out.println(" Turn ");
					//System.out.println(j.toString());
				}
				if(j.getId()==24) {
					//System.out.println(" Turn ");
					//System.out.println(j.toString());
				}
				if(j.getId()==25) {
					//System.out.println(" Turn ");
					//System.out.println(j.toString());
				}

				if(j.getId()==26) {
					//System.out.println(" Turn ");
					////System.out.println(j.toString());
				}
				//System.out.println(" Turn ");
				printing(paramedic);
				boolean insertion=possibleInsertion(j,paramedic);
				if(insertion) {
					break;
				}
			}
		}

		for(Parts paramedic:qualificationParamedic) {
			for(SubJobs j:paramedic.getListSubJobs()) {
				if(qualification<j.getReqQualification()) {
					qualification=j.getReqQualification();
				}
			}
			paramedic.setQualificationParts((int)qualification);
			if(qualification==0) {
				paramedic.setParamedicSchift(true);
			}
			else {
				if(qualification>0) {
					paramedic.setHomecareStaffSchift(true);
				}
			}
		}
		return qualificationParamedic;
	}


	private void printing(Parts paramedic) {
		for(SubJobs j:paramedic.getListSubJobs()) {
			//System.out.println(j.getSubJobKey()+" arrival "+ j.getArrivalTime()+" start time "+ j.getstartServiceTime()+ "req time " + j.getReqTime());
		}
	}


	private boolean possibleInsertion(Jobs j, Parts homeCare) { // si el trabajo se acepta para ser insertado- entonces el trabajo se desagrega en subtrabajos
		boolean inserted=false;
		if(homeCare.getListSubJobs().isEmpty()) {
			insertionJob(j,homeCare);
			printing(homeCare);
			inserted=true;
		}
		else { // inside the array there are more jobs
			// dividir el trabajo
			Parts pickUpDropOff=disaggregatedJob(j);
			// revisar si el primer trabajo de esta parte puede ser insertado
			SubJobs jsplited=null;
			if(j.isClient()) {
				jsplited=pickUpDropOff.getListSubJobs().get(0);
			}
			else {
				jsplited=pickUpDropOff.getListSubJobs().get(0);
			}
			//SubJobs jsplited=pickUpDropOff.getListSubJobs().get(0);

			//double workingHours= computingWorkingHours(homeCare,pickUpDropOff);
			int position=iterateOverSchiftLastPosition(jsplited,pickUpDropOff,homeCare);
			if(position>=0 ) {
				if(jsplited.isClient()) {
					settingEarlyTimeFutureJob(pickUpDropOff);// siempre el trabajo de referencia es el primer nodo
				}
				else {
					if(pickUpDropOff.getListSubJobs().get(0).isPatient()) {
						settingEarlyTimeFuturePatientJob(pickUpDropOff);}
				}
				ArrayList<SubJobs> newListSubJobs= new ArrayList<SubJobs>();
				if(position==0) {

					for(SubJobs ssj:pickUpDropOff.getListSubJobs()) {
						assignedJobs.put(ssj.getSubJobKey(),ssj);
						newListSubJobs.add(ssj);
					}
					for(SubJobs ssj:homeCare.getListSubJobs()) {
						newListSubJobs.add(ssj);
					}

				}
				else if(position==homeCare.getListSubJobs().size()) {
					for(SubJobs ssj:homeCare.getListSubJobs()) {
						newListSubJobs.add(ssj);
					}
					for(SubJobs ssj:pickUpDropOff.getListSubJobs()) {
						assignedJobs.put(ssj.getSubJobKey(),ssj);
						newListSubJobs.add(ssj);
					}
				}
				homeCare.getDirectoryConnections().clear();
				homeCare.getListSubJobs().clear();
				for(SubJobs ssj:newListSubJobs) {
					homeCare.getListSubJobs().add(ssj);
					homeCare.getDirectorySubjobs().put(ssj.getSubJobKey(), ssj);
				}
				printing(homeCare);
				inserted=true;

			}
		}
		return inserted;
	}


	private boolean enoughWorkingHours(Parts homeCare, Parts pickUpDropOff, int position) {
		boolean enoughtTime=false;
		ArrayList<SubJobs> possibleNewRoute= possibleRoute(homeCare,pickUpDropOff,position);


		SubJobs primerInRouteSubJob=possibleNewRoute.get(0);
		SubJobs lastSubJobToInsert=possibleNewRoute.get(possibleNewRoute.size()-1);
		// additional time to insert pickUpDropOff
		double tv=0;
		for(Edge e:pickUpDropOff.getDirectoryConnections().values()) {
			tv+=e.getTime();
		}
		double serviceTime=0;
		for(SubJobs s:pickUpDropOff.getListSubJobs()) {
			serviceTime+=s.getReqTime();
		}
		double distanceBetweenParts=inp.getCarCost().getCost(lastSubJobToInsert.getId()-1,primerInRouteSubJob.getId()-1);		
		double distanceFromDepot=inp.getCarCost().getCost(0,primerInRouteSubJob.getId()-1);
		double distancetoDepot=inp.getCarCost().getCost(lastSubJobToInsert.getId()-1, 0);
		double preliminaryWorkingTime=(homeCare.getListSubJobs().get(homeCare.getListSubJobs().size()-1).getDepartureTime()-homeCare.getListSubJobs().get(0).getArrivalTime())+tv+serviceTime+distanceFromDepot+distancetoDepot+distanceBetweenParts;
		if(preliminaryWorkingTime<test.getWorkingTime()) {
			enoughtTime=true;
		}
		return enoughtTime;
	}

	private ArrayList<SubJobs> possibleRoute(Parts homeCare, Parts pickUpDropOff, int position) {
		ArrayList<SubJobs> possibleNewRoute= new ArrayList<SubJobs>();
		ArrayList<Parts> parts= new ArrayList<Parts>();
		if(position==0) {
			parts.add(homeCare);
			parts.add(position,pickUpDropOff);}
		else {
			parts.add(homeCare);
			parts.add(pickUpDropOff);
		}
		for(Parts p: parts) {
			for(SubJobs j:p.getListSubJobs()) {
				possibleNewRoute.add(j);
			}

		}
		return possibleNewRoute;
	}

	private Parts disaggregatedJob(Jobs j) {
		Parts pickUpDropOff= null; 
		if(j.isClient()) {
			pickUpDropOff=splitClientJobInSubJobs(j);}
		else {
			pickUpDropOff=splitPatientJobInSubJobs(j);
		}
		return pickUpDropOff;
	}

	private double computingWorkingHours(ArrayList<SubJobs> homeCare, ArrayList<SubJobs> pickUpDropOff) {
		double workingTime=0;
		double timeWithPatient=computeService(homeCare,pickUpDropOff);
		double travelTime=computeTravelTime(homeCare,pickUpDropOff);
		workingTime=timeWithPatient+travelTime;
		return workingTime;
	}

	private double computeTravelTime(ArrayList<SubJobs> homeCare, ArrayList<SubJobs> pickUpDropOff) {

		Jobs depot=inp.getNodes().get(0);
		double travelTime=inp.getCarCost().getCost(depot.getId()-1, homeCare.get(0).getId()-1);
		for(int i=0;i<homeCare.size()-1;i++) {
			travelTime+=inp.getCarCost().getCost(homeCare.get(i).getId()-1, homeCare.get(i).getId()-1);
		}
		travelTime+=inp.getCarCost().getCost(homeCare.get(homeCare.size()-1).getId()-1, pickUpDropOff.get(0).getId()-1);
		for(int i=0;i<pickUpDropOff.size()-1;i++) {
			travelTime+=inp.getCarCost().getCost(pickUpDropOff.get(i).getId()-1, pickUpDropOff.get(i).getId()-1);
		}
		travelTime+=inp.getCarCost().getCost(pickUpDropOff.get(0).getId()-1,depot.getId()-1);
		return travelTime;
	}

	private double computeService(ArrayList<SubJobs> homeCare, ArrayList<SubJobs> pickUpDropOff) {
		double timeWithPatient=0;
		double serviceTime=0;
		double loadUnloadingTime=0;
		double registrationTime=0;
		// service time
		for(SubJobs job:homeCare) {
			serviceTime+=job.getReqTime();
			if(job.isClient()) {
				loadUnloadingTime+=test.getloadTimeHomeCareStaff();
			}
			else {
				loadUnloadingTime+=test.getloadTimePatient();
				if(job.isMedicalCentre() && job.getTotalPeople()<0) {
					registrationTime=test.getRegistrationTime();
				}
			}
		}
		// newJobsToInsert
		for(SubJobs job:pickUpDropOff) {
			serviceTime+=job.getReqTime();
			if(job.isClient()) {
				loadUnloadingTime+=test.getloadTimeHomeCareStaff();
			}
			else {
				loadUnloadingTime+=test.getloadTimePatient();
				if(job.isMedicalCentre() && job.getTotalPeople()<0) {
					registrationTime=test.getRegistrationTime();
				}
			}
		}
		timeWithPatient=serviceTime+loadUnloadingTime+registrationTime;
		return timeWithPatient;
	}

	private void insertionJob(Jobs j, Parts homeCare) {
		if(j.isClient()) { // revisar si solo los trabajos j se realizan casa de clientes o en medical centres
			Parts dropOffPickUp=splitClientJobInSubJobs(j);

			insertingSubJobsIntheSequence(homeCare,dropOffPickUp); // la asignación de la lista de subjobs no necesariamente se hace dentro de un mismo turno
			//j.setarrivalTime(j.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		}
		else { // the job j have to be a medical centre
			Parts pickUpDropOff=splitPatientJobInSubJobs(j);
			insertingSubJobsIntheSequence(homeCare,pickUpDropOff); // la asignación de la lista de subjobs no necesariamente se hace dentro de un mismo turno

			//j.setarrivalTime(j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime());
		}
	}

	private void insertingSubJobsIntheSequence(Parts homeCare, Parts dropOffPickUp) {
		if(dropOffPickUp.getListSubJobs().get(0).isClient()) {
			settingEarlyTimeFutureJob(dropOffPickUp);}// siempre el trabajo de referencia es el primer nodo
		else {
			if(dropOffPickUp.getListSubJobs().get(0).isPatient()) {
				settingEarlyTimeFuturePatientJob(dropOffPickUp);}
		}
		for(SubJobs sb:dropOffPickUp.getListSubJobs() ) {
			String key=creatingKey(sb);
			assignedJobs.put(key, sb);
			homeCare.getListSubJobs().add(sb);
			homeCare.getDirectorySubjobs().put(sb.getSubJobKey(), sb);
		}



	}

	private void settingEarlyTimeFuturePatientJob(Parts dropOffPickUp) {
		double tv=inp.getCarCost().getCost(dropOffPickUp.getListSubJobs().get(0).getId()-1, dropOffPickUp.getListSubJobs().get(1).getId()-1);

		//double tv=inp.getCarCost().getCost(dropOffPickUp.getListSubJobs().get(0).getId()-1, dropOffPickUp.getListSubJobs().get(1).getId()-1)*test.getDetour();
		double homeDeparture=dropOffPickUp.getListSubJobs().get(1).getArrivalTime()-tv;

		double homeArrival=homeDeparture-dropOffPickUp.getListSubJobs().get(0).getdeltaArrivalDeparture();
		double homeStartServiceTime=homeArrival+dropOffPickUp.getListSubJobs().get(0).getdeltaArrivalStartServiceTime();
		double departureTime=homeStartServiceTime+dropOffPickUp.getListSubJobs().get(0).getdeltarStartServiceTimeEndServiceTime();
		dropOffPickUp.getListSubJobs().get(0).setarrivalTime(homeArrival);
		dropOffPickUp.getListSubJobs().get(0).setStartServiceTime(homeStartServiceTime);
		dropOffPickUp.getListSubJobs().get(0).setEndServiceTime(departureTime);
		departureTime=dropOffPickUp.getListSubJobs().get(1).getDepartureTime();
		for(int i=2; i<dropOffPickUp.getListSubJobs().size();i++ ) {
			SubJobs sbi=dropOffPickUp.getListSubJobs().get(i-1);
			SubJobs sbj=dropOffPickUp.getListSubJobs().get(i);
			tv= inp.getCarCost().getCost(sbi.getId()-1, sbj.getId()-1);
			//tv= inp.getCarCost().getCost(sbi.getId()-1, sbj.getId()-1)*test.getDetour();
			double arrivalTime=departureTime+tv;

			double startServiceTime=arrivalTime+sbj.getdeltaArrivalStartServiceTime();
			departureTime=arrivalTime+sbj.getdeltaArrivalDeparture();
			double endServiceTime=startServiceTime+sbj.getdeltarStartServiceTimeEndServiceTime();
			sbj.setarrivalTime(arrivalTime);
			sbj.setStartServiceTime(startServiceTime);
			sbj.setEndServiceTime(endServiceTime);
			sbj.setdepartureTime(departureTime);	
			// setting new tw
			sbj.setStartTime(startServiceTime);
			sbj.setEndTime(startServiceTime);
		}
		//System.out.println(dropOffPickUp.toString());	
	}


	private void settingEarlyTimeFutureJob(Parts dropOffPickUp) {

		double endServiceTime=dropOffPickUp.getListSubJobs().get(0).getendServiceTime();
		for(int i=1; i<dropOffPickUp.getListSubJobs().size();i++ ) {
			SubJobs sbi=dropOffPickUp.getListSubJobs().get(i-1);
			SubJobs sbj=dropOffPickUp.getListSubJobs().get(i);
			//double tv= inp.getCarCost().getCost(sbi.getId()-1, sbj.getId()-1)*test.getDetour();
			double tv= inp.getCarCost().getCost(sbi.getId()-1, sbj.getId()-1);

			double arrivalTime=endServiceTime+tv;

			double startServiceTime=arrivalTime+sbj.getdeltaArrivalStartServiceTime();
			double departureTime=arrivalTime+sbj.getdeltaArrivalDeparture();
			endServiceTime=startServiceTime+sbj.getdeltarStartServiceTimeEndServiceTime();
			sbj.setarrivalTime(arrivalTime);
			sbj.setStartServiceTime(startServiceTime);
			sbj.setEndServiceTime(endServiceTime);
			sbj.setdepartureTime(departureTime);
			// definición de las nuevas ventanas de tiempo
			sbj.setStartTime(startServiceTime);
			sbj.setEndTime(startServiceTime);
		}
		//System.out.println(dropOffPickUp.toString());	
	}

	private String creatingKey(SubJobs sb) {
		String key="";
		if(sb.getTotalPeople()>0) {
			key="P"+sb.getId();
		}
		else {
			key="D"+sb.getId();
		}
		if(sb.isMedicalCentre()) {
			key=key+sb.getIdUser();
		}
		return key;
	}

	private Parts splitPatientJobInSubJobs(Jobs j) {

		if(j.getSubJobKey().equals("D653")) {
			//System.out.println("Stop");
		}
		if(j.getSubJobKey().equals("D653")) {
			//System.out.println("Stop");
		}
		if(j.getId()==3) {
			//System.out.println("Stop");
		}
		if(j.getId()==3) {
			//System.out.println("Stop");
		}

		Parts newPart= new Parts();
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();// considerar el inicio y el fin del servicio
		// 2. Generation del drop off at medical centre
		//		Jobs j1patient=	coupleList.get(j.getSubJobKey()).getPresent();
		//		Jobs j1dropOffMedicalCentre=	coupleList.get(j.getSubJobKey()).getFuture();
		Couple c=coupleList.get(j.getSubJobKey());
		Jobs patient= new Jobs(c.getPresent());
		SubJobs dropOffMedicalCentre= new SubJobs(c.getFuture());
		dropOffMedicalCentre.setMedicalCentre(true);
		dropOffMedicalCentre.setPatient(false);
		//settingTimeDropOffPatientParamedicSubJob(dropOffMedicalCentre,j);


		// 1. Generation del pick up at patient home 
		SubJobs pickUpPatientHome= new SubJobs(c.getPresent());
		pickUpPatientHome.setPatient(true);
		pickUpPatientHome.setMedicalCentre(false);
		pickUpPatientHome.setPair(dropOffMedicalCentre);
		dropOffMedicalCentre.setIdUser(pickUpPatientHome.getId());
		Couple c0= new Couple(pickUpPatientHome,dropOffMedicalCentre);
		//	settingTimePickUpPatientSubJob(pickUpPatientHome,dropOffMedicalCentre);
		//Couple n=new Couple(pickUpPatientHome, dropOffMedicalCentre);
		//		coupleList.put(pickUpPatientHome.getSubJobKey(), n);
		//		coupleList.put(dropOffMedicalCentre.getSubJobKey(), n);
		dropoffpatientMedicalCentre.put(dropOffMedicalCentre.getSubJobKey(), c0);
		coverageMatrix[pickUpPatientHome.getId()-1][dropOffMedicalCentre.getId()-1]=1;
		//dropOffMedicalCentre.setPair(pickUpPatientHome);
		// 3. Generación del pick at medical centre
		String key="D"+patient.getId();
		Couple c1=coupleList.get(key);
		SubJobs pickUpMedicalCentre= new SubJobs(c1.getPresent());
		pickUpMedicalCentre.setMedicalCentre(true);
		pickUpMedicalCentre.setPatient(false);
		//settingTimePickUpPatientParamedicSubJob(pickUpMedicalCentre,dropOffMedicalCentre);

		// 4. Generación del drop-off at client home
		SubJobs dropOffPatientHome= new SubJobs(c1.getFuture());
		dropOffPatientHome.setPatient(true);

		dropOffPatientHome.setMedicalCentre(false);
		dropOffPatientHome.setPair(pickUpMedicalCentre);
		pickUpMedicalCentre.setIdUser(dropOffPatientHome.getId());


		if(c1.getPresent().getSubJobKey().equals("D57")) {
			if(c1.getPresent().isMedicalCentre()) {
				//System.out.println(c1.getPresent().toString());
			}
		}
		if(c1.getFuture().getSubJobKey().equals("D57")) {
			if(c1.getFuture().isMedicalCentre()) {
				//System.out.println(c1.getFuture().toString());
			}
		}

		Couple c2=new Couple(pickUpMedicalCentre,dropOffPatientHome);
		//	settingTimeDropOffPatientSubJob(dropOffPatientHome,pickUpMedicalCentre);
		//n=new Couple(pickUpMedicalCentre, dropOffPatientHome);
		//		coupleList.put(pickUpMedicalCentre.getSubJobKey(), n);
		//		coupleList.put(dropOffPatientHome.getSubJobKey(), n);
		pickpatientMedicalCentre.put(pickUpMedicalCentre.getSubJobKey(), c2);
		//dropOffPatientHome.setPair(pickUpMedicalCentre);
		// 3. Addding the subjobs to the list
		subJobsList.add(pickUpPatientHome); // Se apilan por orden de sequencia
		subJobsList.add(dropOffMedicalCentre);
		subJobsList.add(pickUpMedicalCentre); // Se apilan por orden de sequencia
		subJobsList.add(dropOffPatientHome);
		newPart.setListSubJobs(subJobsList, inp, test);
		if(c2.getPresent().getSubJobKey().equals("D57")) {
			if(c2.getPresent().isMedicalCentre()) {
				//System.out.println(c2.getPresent().toString());
			}
		}
		if(c2.getFuture().getSubJobKey().equals("D57")) {
			if(c2.getFuture().isMedicalCentre()) {
				//System.out.println(c2.getFuture().toString());
			}
		}
		return newPart;
	}

	private void settingTimeDropOffPatientSubJob(SubJobs dropOffPatientHome, Jobs pickUpMedicalCentre) {
		//()--------------(j)-------------(dropOffPatientHome)
		dropOffPatientHome.setTotalPeople(-1);
		dropOffPatientHome.setPatient(true);
		dropOffPatientHome.setMedicalCentre(false);
		dropOffPatientHome.setloadUnloadTime(test.getloadTimePatient());
		// 1. Setting the start service time -- startServiceTime
		double travel=inp.getCarCost().getCost(pickUpMedicalCentre.getId()-1, dropOffPatientHome.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones
		double arrivalTime=pickUpMedicalCentre.getDepartureTime()+travel;  // load al lugar de la cita medica
		dropOffPatientHome.setarrivalTime(arrivalTime);
		dropOffPatientHome.setStartTime(arrivalTime);
		dropOffPatientHome.setEndTime(arrivalTime);// departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al vehículo
		// modificar el tiempo requerido para el trabajo+
		dropOffPatientHome.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		dropOffPatientHome.setStartServiceTime(dropOffPatientHome.getEndTime());
		// 3. Set el fin del servicio
		dropOffPatientHome.setEndServiceTime(dropOffPatientHome.getstartServiceTime()+test.getloadTimePatient());
		dropOffPatientHome.setdepartureTime(dropOffPatientHome.getendServiceTime());
		if(dropOffPatientHome.getSubJobKey().equals("D57")) {
			if(dropOffPatientHome.isMedicalCentre()) {
				//System.out.println(dropOffPatientHome.toString());
			}
		}
		if(dropOffPatientHome.getSubJobKey().equals("D57")) {
			if(dropOffPatientHome.isMedicalCentre()) {
				//System.out.println(dropOffPatientHome.toString());
			}
		}

	}

	private void settingTimePickUpPatientParamedicSubJob(SubJobs pickUpMedicalCentre, Jobs j) {
		//()--------------(pickUpMedicalCentre=j)-------------()-------------()
		pickUpMedicalCentre.setTotalPeople(2); // 5. Setting the total people (+) pick up   (-) drop-off
		pickUpMedicalCentre.setMedicalCentre(true);
		pickUpMedicalCentre.setPatient(false);
		pickUpMedicalCentre.setloadUnloadTime(test.getloadTimePatient());
		pickUpMedicalCentre.setserviceTime(j.getReqTime());
		j.setserviceTime(0);
		pickUpMedicalCentre.setStartTime(j.getDepartureTime()+pickUpMedicalCentre.getReqTime());
		pickUpMedicalCentre.setEndTime(j.getDepartureTime()+pickUpMedicalCentre.getReqTime()+test.getCumulativeWaitingTime());
		// 1. Setting the start service time -- startServiceTime
		pickUpMedicalCentre.setStartServiceTime(pickUpMedicalCentre.getEndTime());
		pickUpMedicalCentre.setarrivalTime(pickUpMedicalCentre.getstartServiceTime());
		// 3. Set el fin del servicio
		pickUpMedicalCentre.setEndServiceTime(pickUpMedicalCentre.getstartServiceTime()+test.getloadTimePatient());
		pickUpMedicalCentre.setdepartureTime(pickUpMedicalCentre.getendServiceTime());
	}

	private void settingTimeDropOffPatientParamedicSubJob(SubJobs dropOffMedicalCentre, Jobs j) { // hard time window
		//()-------------(dropOffMedicalCentre=j)-------------()-------------()
		dropOffMedicalCentre.setTotalPeople(-2);	// 5. Setting the total people (+) pick up   (-) drop-off
		dropOffMedicalCentre.setloadUnloadTime(test.getloadTimePatient());
		dropOffMedicalCentre.setloadUnloadRegistrationTime(test.getRegistrationTime());
		dropOffMedicalCentre.setMedicalCentre(true);
		dropOffMedicalCentre.setStartServiceTime(j.getEndTime());// 1. Setting the start service time -- startServiceTime
		// 2. Set ArrivalTime
		dropOffMedicalCentre.setarrivalTime(dropOffMedicalCentre.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime()); // el tiempo del registro
		dropOffMedicalCentre.setEndServiceTime(dropOffMedicalCentre.getstartServiceTime());	
		dropOffMedicalCentre.setdepartureTime(dropOffMedicalCentre.getendServiceTime());

		if(dropOffMedicalCentre.getSubJobKey().equals("D57")) {
			if(dropOffMedicalCentre.isMedicalCentre()) {
				//System.out.println(dropOffMedicalCentre.toString());
			}
		}
		if(dropOffMedicalCentre.getSubJobKey().equals("D57")) {
			if(dropOffMedicalCentre.isMedicalCentre()) {
				//System.out.println(dropOffMedicalCentre.toString());
			}
		}
		// delta time
		//		double deltaArrivalDeparture=dropOffMedicalCentre.getDepartureTime()-dropOffMedicalCentre.getArrivalTime();
		//		double deltaArrivalStartServiceTime=dropOffMedicalCentre.getstartServiceTime()-dropOffMedicalCentre.getArrivalTime();
		//		double deltarStartServiceTimeEndServiceTime=dropOffMedicalCentre.getendServiceTime()-dropOffMedicalCentre.getstartServiceTime();
		//		dropOffMedicalCentre.setdeltaArrivalDeparture(deltaArrivalDeparture);
		//		dropOffMedicalCentre.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		//		dropOffMedicalCentre.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);


	}

	private void settingTimePickUpPatientSubJob(SubJobs pickUpPatientHome, Jobs j) { // j <- es el nodo en donde se tiene la cita medica
		//(pickUpPatientHome)--------------(j)-------------()-------------()
		// 1. Setting the start service time -- startServiceTime
		// 5. Setting the total people (+) pick up   (-) drop-off
		pickUpPatientHome.setTotalPeople(1);
		pickUpPatientHome.setPatient(true);
		pickUpPatientHome.setMedicalCentre(false);
		pickUpPatientHome.setloadUnloadTime(test.getloadTimePatient());
		double travel=inp.getCarCost().getCost(pickUpPatientHome.getId()-1, j.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones
		double departureTimeAtMedicalCentre=j.getArrivalTime()-travel;  // load al lugar de la cita medica
		pickUpPatientHome.setEndServiceTime(departureTimeAtMedicalCentre);
		pickUpPatientHome.setdepartureTime(departureTimeAtMedicalCentre);	

		pickUpPatientHome.setarrivalTime(departureTimeAtMedicalCentre-test.getloadTimePatient());
		pickUpPatientHome.setStartTime(departureTimeAtMedicalCentre); // setting time window
		pickUpPatientHome.setEndTime(departureTimeAtMedicalCentre);
		// modificar el tiempo requerido para el trabajo+
		pickUpPatientHome.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		pickUpPatientHome.setStartServiceTime(pickUpPatientHome.getEndTime());
		// 3. Set el fin del servicio


		//		// delta tiempo
		//		double deltaArrivalDeparture=pickUpPatientHome.getDepartureTime()-pickUpPatientHome.getArrivalTime();
		//		double deltaArrivalStartServiceTime=pickUpPatientHome.getstartServiceTime()-pickUpPatientHome.getArrivalTime();
		//		double deltarStartServiceTimeEndServiceTime=pickUpPatientHome.getendServiceTime()-pickUpPatientHome.getstartServiceTime();
		//		pickUpPatientHome.setdeltaArrivalDeparture(deltaArrivalDeparture);
		//		pickUpPatientHome.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		//		pickUpPatientHome.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);
	}

	private Parts splitClientJobInSubJobs(Jobs j) {
		Parts newParts= new Parts();
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();// considerar el inicio y el fin del servicio
		// 1. Generación del drop-off job
		if(j.getSubJobKey().equals("D24")) {
			//System.out.println(j.toString());
		}
		Couple c=coupleList.get(j.getSubJobKey());
		SubJobs dropOff= new SubJobs(c.getPresent());
		dropOff.setClient(true);
		// 2. Generación del pick-up job
		SubJobs pickUp= new SubJobs(c.getFuture());
		pickUp.setClient(true);
		Couple c1= new Couple(dropOff,pickUp);
		//settingTimeClientSubJob(dropOff,pickUp);
		// 3. Addding the subjobs to the list
		subJobsList.add(dropOff); // Se apilan por orden de sequencia
		subJobsList.add(pickUp);
		//Couple n=new Couple(dropOff,pickUp);
		//	coupleList.put(dropOff.getSubJobKey(), n);
		//coupleList.put(pickUp.getSubJobKey(), n);
		dropoffHomeCareStaff.put(dropOff.getSubJobKey(), c1);
		pickUpHomeCareStaff.put(pickUp.getSubJobKey(), c1);

		coverageMatrix[dropOff.getId()-1][pickUp.getId()-1]=1;
		newParts.setListSubJobs(subJobsList,inp,test);
		return newParts;
	}

	private void settingTimeClientSubJob(SubJobs dropOff, SubJobs pickUp) {
		homeCareDropOff(dropOff);
		homeCarePickUp(dropOff,pickUp);

		// delta time: present
		//		double deltaArrivalDeparture=dropOff.getDepartureTime()-dropOff.getArrivalTime();
		//		double deltaArrivalStartServiceTime=dropOff.getstartServiceTime()-dropOff.getArrivalTime();
		//		double deltarStartServiceTimeEndServiceTime=dropOff.getendServiceTime()-dropOff.getstartServiceTime();
		//		dropOff.setdeltaArrivalDeparture(deltaArrivalDeparture);
		//		dropOff.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		//		dropOff.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);
		// delta tiempo: future
		//		deltaArrivalDeparture=pickUp.getDepartureTime()-pickUp.getArrivalTime();
		//		deltaArrivalStartServiceTime=pickUp.getstartServiceTime()-pickUp.getArrivalTime();
		//		deltarStartServiceTimeEndServiceTime=pickUp.getendServiceTime()-pickUp.getstartServiceTime();
		//		pickUp.setdeltaArrivalDeparture(deltaArrivalDeparture);
		//		pickUp.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		//		pickUp.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);

	}

	private void homeCarePickUp(SubJobs dropOff, SubJobs pickUp) {
		pickUp.setTotalPeople(1);
		pickUp.setClient(true);	
		pickUp.setserviceTime(dropOff.getReqTime()); //los nodos pick up contienen la información de los nodos
		dropOff.setserviceTime(0);
		// Setting the TW
		pickUp.setStartTime(dropOff.getendServiceTime()+pickUp.getReqTime());
		pickUp.setEndTime(dropOff.getendServiceTime()+pickUp.getReqTime()+test.getCumulativeWaitingTime()); // considering waiting time
		// modificar el tiempo requerido para el trabajo+	
		// 1. Setting the start service time -- startServiceTime
		pickUp.setStartServiceTime(pickUp.getEndTime());
		// 3. Set el fin del servicio
		pickUp.setEndServiceTime(pickUp.getstartServiceTime()+test.getloadTimeHomeCareStaff());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		pickUp.setarrivalTime(pickUp.getStartTime());
		pickUp.setdepartureTime(pickUp.getendServiceTime());
		// 5. Setting the total people (+) pick up   (-) drop-off
	}


	private void homeCareDropOff(SubJobs dropOff) {


		dropOff.setClient(true);
		// 1. Setting the start service time -- startServiceTime
		dropOff.setStartServiceTime(dropOff.getEndTime());
		// 2. Set ArrivalTime
		dropOff.setarrivalTime(dropOff.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		// 3. Set el fin del servicio
		dropOff.setEndServiceTime(dropOff.getstartServiceTime());	
		dropOff.setdepartureTime(dropOff.getendServiceTime());
		// 5. Setting the total people (+) pick up   (-) drop-off	
	}


	private double computingEndWorkingDay(Jobs lastJob, Jobs depot) {
		double travelTime=inp.getCarCost().getCost(lastJob.getId()-1,depot.getId()-1);
		double loadTime=0;
		if(lastJob.isClient()) {
			loadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadTime=test.getloadTimePatient();

		}
		double arrivalTimeToTheDepot= lastJob.getendServiceTime()+travelTime+loadTime; // cuando sale el personal del deposito es cuando empieza a contar el tiempo

		return arrivalTimeToTheDepot;
	}

	private double computingStartWorkingDay(Jobs depot, Jobs firstJob) {
		double travelTime=inp.getCarCost().getCost(depot.getId()-1,firstJob.getId()-1);
		double loadTime=0;
		if(firstJob.isClient()) {
			loadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadTime=test.getloadTimePatient()+test.getRegistrationTime();

		}
		double arrivalTimeToTheNode= firstJob.getstartServiceTime()-loadTime;
		double departureTimeToTheDepot= arrivalTimeToTheNode-travelTime; // cuando sale el personal del deposito es cuando empieza a contar el tiempo
		return departureTimeToTheDepot;
	}

	private int iterateOverRoute(SubJobs j, Route r) {
		int position=-1;
		boolean inserted=false;
		for(int i=0;i<r.getSubJobsList().size();i++) {
			SubJobs inRoute=r.getSubJobsList().get(i);
			if(i==r.getSubJobsList().size()-1) {
				inserted=insertionLaterVehicle(inRoute,j);//(inRoute)******(j)
				if(inserted) {
					position=r.getSubJobsList().size();
				}
				else{
					if(i==0) {
						inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
						if(inserted) {
							position=0;
						}	
					}					
				}
			}
			else {// // (inRoute)*******(j)******(inRouteK)
				if(i==0) {
					inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
					if(inserted) {
						position=0;

					}
				}
				if(!inserted) {
					SubJobs inRouteK=r.getSubJobsList().get(i+1);
					inserted=insertedMiddleVehicle(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
					if(inserted) {
						position=i+1;
					}
				}
			}
			r.updatingJobsList();
		}
		return position;
	}




	private int iterateOverSchift(SubJobs j, Parts homeCare) {
		if(j.getSubJobKey().equals("D54")) {
			//System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());	
		}
		//System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteración sobre el turno y se inserta
		//ArrayList<SubJobs> homeCare=copyListJobs(currentJobs);
		for(int i=0;i<homeCare.getListSubJobs().size();i++) { 
			SubJobs inRoute=homeCare.getListSubJobs().get(i);
			if(i==homeCare.getListSubJobs().size()-1 ) {
				inserted=insertionLater(inRoute,j);//(inRoute)******(j)
				if(!inserted) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
					if(inserted) {
						position=i-1;	
						break;
					}
				}
				else {
					position=i+1;
					break;
				}
			}
			else {// // (inRoute)*******(j)******(inRouteK)
				if(i==0) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
					if(inserted) {
						position=0;
						break;
					}
				}
				if(!inserted) {
					SubJobs inRouteK=homeCare.getListSubJobs().get(i+1);
					inserted=insertedMiddleJob(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
					if(inserted) {
						position=i+1;
						break;
					}
				}
			}
		}
		return position;
	}

	private int iterateOverSchiftLastPosition(SubJobs j, Parts pickUpDropOff, Parts homeCare) {
		//System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());

		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteración sobre el turno y se inserta
		//ArrayList<SubJobs> homeCare=copyListJobs(currentJobs);
		SubJobs inRoute=homeCare.getListSubJobs().get(homeCare.getListSubJobs().size()-1);
		inserted=insertionLater(inRoute,j);//(inRoute)******(j)
		if(inserted) {
			position=homeCare.getListSubJobs().size();
			if(enoughWorkingHours(homeCare,pickUpDropOff,position)) {
				inserted=true;
			}
			else {inserted=false;
			position=-1;}
		}
		else {
			SubJobs firstInRoute=homeCare.getListSubJobs().get(0);
			position=insertionChangingStartServiceTime(firstInRoute,pickUpDropOff);
			if(position>=0) {
				if(enoughWorkingHours(homeCare,pickUpDropOff,position)) {
					inserted=true;
				}
				else {inserted=false;
				position=-1;}
			}}
		return position;
	}


	private int insertionChangingStartServiceTime(SubJobs firstInRoute, Parts pickUpDropOff) {
		boolean inserted=false;
		int position=-1;
		SubJobs j=pickUpDropOff.getListSubJobs().get(0);
		SubJobs k=pickUpDropOff.getListSubJobs().get(pickUpDropOff.getListSubJobs().size()-1);
		if(j.isClient() && k.isClient() ) {
			// se intenta insertar antes - El trabajo importante es j porque k es la continuación
			//double tv=inp.getCarCost().getCost(k.getId()-1, firstInRoute.getId()-1)*test.getDetour();
			double tv=inp.getCarCost().getCost(k.getId()-1, firstInRoute.getId()-1);
			double departureK=firstInRoute.getArrivalTime()-tv;

			double arrivalTimeK=departureK-k.getdeltaArrivalDeparture();
			double startServiceTimeK=arrivalTimeK+k.getdeltaArrivalStartServiceTime();

			double endServiceTimeK=startServiceTimeK+k.getdeltarStartServiceTimeEndServiceTime();
			double hardStartServiceTime=arrivalTimeK-j.getReqTime();
			if(hardStartServiceTime>j.getStartTime() && hardStartServiceTime<j.getEndTime()) {
				position=0;
				double deltaStartServiceTime=j.getstartServiceTime()-hardStartServiceTime;
				j.setStartServiceTime(hardStartServiceTime);
				j.setarrivalTime(j.getstartServiceTime()-j.getdeltaArrivalStartServiceTime());
				j.setdepartureTime(j.getArrivalTime()+j.getdeltaArrivalDeparture());
				j.setEndServiceTime(j.getstartServiceTime()+j.getdeltarStartServiceTimeEndServiceTime());
				//				for(SubJobs sj:pickUpDropOff.getListSubJobs()) {
				//					sj.setarrivalTime(sj.getArrivalTime()-deltaStartServiceTime);
				//					sj.setStartServiceTime(sj.getstartServiceTime()-deltaStartServiceTime);
				//					sj.setEndServiceTime(sj.getendServiceTime()-deltaStartServiceTime);
				//					sj.setdepartureTime(sj.getDepartureTime()-deltaStartServiceTime);
				//					if(sj.isClient() && sj.getTotalPeople()>0) {
				//						sj.setStartTime(sj.getstartServiceTime());
				//						sj.setEndTime(sj.getstartServiceTime());
				//					}
				//				}	
			}
		}
		return position;
	}



	private void missingSubJobs(Parts pickUpDropOff) {
		for(SubJobs sj:pickUpDropOff.getListSubJobs()) {
			if(!assignedJobs.containsKey(sj.getSubJobKey())) {
				iteratingOverCurrentShifts(sj);
			}

		}


	}

	private void iteratingOverCurrentShifts(SubJobs sj) {
		// ArrayList<ArrayList<SubJobs>> qualificationParamedic= new ArrayList<>(); // turn for paramedics
		int position=-1;
		for(Parts schift:qualificationParamedic) {
			if(schift.getListSubJobs().isEmpty()) {
				insertionJob(sj,schift);
				printing(schift);
			}
			else {
				position=iterateOverSchift(sj,schift);
				//	position=iterateOverSchiftLastPosition(sj,schift);
				if(position>0) {
					schift.getListSubJobs().add(position,sj);
					printing(schift);
					assignedJobs.put(sj.getSubJobKey(),sj);
					break;
				}}
		}
	}



	private boolean insertedMiddleVehicle(SubJobs inRoute, SubJobs j, SubJobs inRouteK) {
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(j,inRouteK);
		double possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromNextNode(j,inRouteK,tv);
		double possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
		double possibleDepartureTimeTime=computeDepartureTime(j,possibleArrivalTime);// considering the registrationTime
		if(possibleArrivalTime>=inRoute.getDepartureTime() && possibleDepartureTimeTime<=inRouteK.getArrivalTime() &&  possibleArrivalTime<=j.getEndTime()) {
			if(inRouteK.getstartServiceTime()>j.getstartServiceTime()) {	
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		if(!inserted) {
			tv=computingTravelTimeWithDetour(inRoute,j);
			possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromPreviousNode(inRoute,tv);
			possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
			possibleDepartureTimeTime=computeDepartureTime(j,possibleArrivalTime);// considering the registrationTime
			if(possibleArrivalTime>inRoute.getDepartureTime() && possibleDepartureTimeTime<=inRouteK.getArrivalTime() && possibleArrivalTime<=j.getEndTime()) {
				if(inRouteK.getstartServiceTime()>j.getstartServiceTime()) {
					settingTimes(possibleArrivalTime,j);
					inserted=true;}
			}
		}

		return inserted;
	}

	private double computeDepartureTime(Jobs j, double possibleArrivalTime) {
		double departure=possibleArrivalTime;
		if(j.isClient()) {
			departure+=test.getloadTimeHomeCareStaff();
		}
		else {
			departure+=test.getloadTimePatient();
		}
		return departure;
	}

	private boolean insertedMiddleJob(SubJobs inRoute, SubJobs j, SubJobs inRouteK) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRouteK.getId()-1);
		double additionalTime=computeAdditionalTime(inRouteK);
		double possibleArrivalTime=inRouteK.getstartServiceTime()-(additionalTime+tv);
		if(possibleArrivalTime>=inRoute.getstartServiceTime() && possibleArrivalTime>0) {
			if(possibleArrivalTime<=j.getEndTime()) {
				if(j.getstartServiceTime()>=inRoute.getstartServiceTime() && j.getstartServiceTime()<=inRouteK.getstartServiceTime()) {
					inserted=true;}
			}
			else {
				tv=inp.getCarCost().getCost(inRoute.getId()-1,j.getId()-1);
				possibleArrivalTime=inRoute.getstartServiceTime()+(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
				if(possibleArrivalTime<=inRouteK.getstartServiceTime() && possibleArrivalTime>0) {
					if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
						if(j.getstartServiceTime()>=inRoute.getstartServiceTime() && j.getstartServiceTime()<=inRouteK.getstartServiceTime()) {
							settingTimes(possibleArrivalTime,j);
							inserted=true;
						}
					}
				}
			}
		}
		else {
			tv=inp.getCarCost().getCost(inRoute.getId()-1,j.getId()-1);
			possibleArrivalTime=inRoute.getstartServiceTime()+(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
			if(possibleArrivalTime<=inRouteK.getstartServiceTime() && possibleArrivalTime>0) {
				if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
					if(j.getstartServiceTime()>=inRoute.getstartServiceTime() && j.getstartServiceTime()<=inRouteK.getstartServiceTime()) {
						settingTimes(possibleArrivalTime,j);
						inserted=true;
					}
				}
			}
		}

		return inserted;
	}

	private boolean insertionEarly(SubJobs inRoute, SubJobs j) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRoute.getId()-1);
		double additionalTime=computeAdditionalTime(inRoute);
		double possibleArrivalTime=inRoute.getstartServiceTime()-(additionalTime+tv);
		if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
			if(inRoute.getstartServiceTime()>=j.getstartServiceTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		return inserted;
	}

	private boolean insertionEarlyVehicle(SubJobs inRoute, SubJobs j) {//(j)******(inRoute)
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(j,inRoute);
		// from the next node
		double possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromNextNode(j,inRoute,tv);// considering the registrationTime;
		double possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
		if(possibleStartServiceTime<=j.getEndTime()) { // se considera menor para considerar los tiempos de espera
			if(inRoute.getstartServiceTime()>=j.getStartTime() || inRoute.getstartServiceTime()>=j.getEndTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		return inserted;
	}


	private double computeArrivalTimeAtMedicalCentreFromNextNode(Jobs j, Jobs inRoute, double tv) {
		double possibleArrivalTime=inRoute.getArrivalTime()-(tv);
		if(j.isClient()) {
			possibleArrivalTime-=test.getloadTimeHomeCareStaff();
		}
		else {
			possibleArrivalTime-=test.getloadTimePatient()-test.getRegistrationTime();
		}
		return possibleArrivalTime;

	}

	private double computingTravelTimeWithDetour(SubJobs inRoute, SubJobs j) {
		double travelTime=inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1);
		//double travelTime=inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1)*test.getDetour();
		return travelTime;
	}

	private void settingTimes(double possibleArrivalTime, SubJobs j) {
		// setting arrival time
		j.setarrivalTime(possibleArrivalTime);
		// setting departure time
		double additionalTime=computeAdditionalTime(j);
		j.setWaitingTime(possibleArrivalTime+additionalTime, j.getstartServiceTime());	
		j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
	}

	private boolean insertionLater(SubJobs inRoute,SubJobs j) {//(inRoute)******(j)
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(inRoute,j);// considering the detour
		double possibleArrivalTime=inRoute.getendServiceTime()+tv; // the possible arrival time have to be lower than the end time of the tw of the nodo
		if(possibleArrivalTime<=j.getstartServiceTime()) {
			inserted=true;
			settingTimes(possibleArrivalTime,j);
			///j.setWaitingTime(possibleArrivalTime, j.getstartServiceTime());
		}
		else{
			if((possibleArrivalTime+j.getloadUnloadRegistrationTime()+j.getloadUnloadTime())<=j.getEndTime()) {
				//if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>=j.getStartTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;

			}
		}
		return inserted;
	}



	private boolean insertionLaterVehicle(SubJobs inRoute,SubJobs j) {//(inRoute)******(j)
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(inRoute,j);// considering the detour
		// from previous node inRoute
		double possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromPreviousNode(inRoute,tv);// considering the registrationTime
		double possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
		if(possibleStartServiceTime<=j.getEndTime()) {
			if(inRoute.getstartServiceTime()<=j.getStartTime() || inRoute.getstartServiceTime()<=j.getEndTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		// from the node to insert node j
		if(!inserted) {// the job is not inserted
			possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromCurrentNode(inRoute,j,tv);// considering the registrationTime
			possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
			if(possibleStartServiceTime<=j.getEndTime()) {
				if(inRoute.getstartServiceTime()<=j.getStartTime() || inRoute.getstartServiceTime()<=j.getEndTime()) {
					settingTimes(possibleArrivalTime,j);
					inserted=true;
				}
			}	
		}

		return inserted;
	}

	private double computeArrivalTimeAtMedicalCentreFromCurrentNode(Jobs inRoute, Jobs j, double tv) {
		double possibleArrivalTime=j.getstartServiceTime();
		if(j.isClient()) {
			possibleArrivalTime=j.getstartServiceTime()-test.getloadTimeHomeCareStaff();
		}
		else {
			possibleArrivalTime=j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime();
		}
		return possibleArrivalTime;
	}

	private double computeStartServiceAtMedicalCentre(Jobs j, double possibleArrivalTime, double tv) {
		double possibleStartServiceTime=possibleArrivalTime;
		if(j.isClient()) {
			possibleStartServiceTime+=test.getloadTimeHomeCareStaff();
		}
		else {
			possibleStartServiceTime+=test.getRegistrationTime()+test.getloadTimePatient();
		}
		double startServiceTime=Math.max(j.getstartServiceTime(), possibleStartServiceTime);
		return startServiceTime;
	}

	private double computeArrivalTimeAtMedicalCentreFromPreviousNode(Jobs inRoute, double tv) { //(inRoute)******(j)
		double possibleArrivalTime=inRoute.getArrivalTime()+tv; 
		if(inRoute.getTotalPeople()>0) { // the load time here is for the node which are already in the route
			possibleArrivalTime+=test.getloadTimeHomeCareStaff();
		}
		return possibleArrivalTime;
	}

	private void settingStartServiceTime() {
		for(Couple couple:subJobsList) {
			double serviceTimePresent=couple.getPresent().getEndTime();
			double serviceTimeFuture=couple.getFuture().getEndTime();
			if(couple.getPresent().getId()==37 ||couple.getFuture().getId()==37  ) {
				//System.out.println("Stop");
			}
			couple.getPresent().setStartServiceTime(serviceTimePresent);
			couple.getFuture().setStartServiceTime(serviceTimeFuture);


		}

	}




	private  ArrayList<ArrayList<Couple>> clasificationjob() {
		ArrayList<ArrayList<Couple>> clasification= new ArrayList<ArrayList<Couple>>();
		// 0.classified couples according the req qualification
		//		int i=-1;
		//		for(Couple c:coupleList) {
		//			i++;
		//			coupleList.put(i, c.getPresent());
		//
		//			i++;
		//			subJobs.put(i, c.getFuture());
		//
		//			if(c.getPresent().getReqQualification()!=c.getFuture().getReqQualification()) {
		//				System.out.print("Error");	
		//			}
		//		}

		for(int qualification=0;qualification<=inp.getMaxQualificationLevel();qualification++) {
			for(Couple c:coupleList.values()) {
				if(c.getFuture().getId()==37 || c.getPresent().getId()==37) {
					System.out.print("Error");	
				}
				if(c.getQualification()==qualification && qualification==0) {
					if(inp.getpatients().containsKey(c.getFuture().getId())) {
						c.getFuture().setPatient(true);
						c.getPresent().setMedicalCentre(false);
					}
					if(inp.getMedicalCentre().containsKey(c.getFuture().getId())) {
						c.getPresent().setMedicalCentre(true);
						c.getFuture().setPatient(false);}
					subJobspatients.add(c);

					if(c.getPresent().getSubJobKey().equals("D57")) {
						if(c.getPresent().isMedicalCentre()) {
							//System.out.println("Stop");
						}
					}
					if(c.getFuture().getSubJobKey().equals("D57")) {
						if(c.getFuture().isMedicalCentre()) {
							//System.out.println("Stop");
						}
					}

				}
				if(c.getQualification()==qualification && qualification==1) {
					c.getFuture().setClient(true);
					c.getPresent().setClient(true);
					subJobsLowestQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==2) {
					c.getFuture().setClient(true);
					c.getPresent().setClient(true);
					this.subJobsMediumQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==3) {
					c.getFuture().setClient(true);
					c.getPresent().setClient(true);
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
		}
	}


	// getters 
	public Solution getInitialSol() {return initialSol;}
	public Solution getSol() {return solution;}
	public HashMap<String, Couple> getCoupleList() {return coupleList;}
	public  HashMap<Integer, SubRoute> getobsInWalkingRoute(){return jobsInWalkingRoute;}



}
