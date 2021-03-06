import java.util.ArrayList;
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
	private  ArrayList<Route> routeList= new ArrayList<Route>(); // dummy routes
	private  HashMap<Integer, Jobs> subJobs= new HashMap<>();


	private  HashMap<String, Couple> dropoffHomeCareStaff= new HashMap<>();// hard time windows list of home care staff 
	private  HashMap<String, Couple> dropoffpatientMedicalCentre= new HashMap<>();// hard time windows list of patient
	private  HashMap<String, Couple> pickpatientMedicalCentre= new HashMap<>();// soft time windows list of patient
	private  HashMap<String, Couple> pickUpHomeCareStaff= new HashMap<>();// soft time windows list of home care staff 



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
	private HashMap<Integer, Jobs>jobsVehicle=new HashMap<Integer, Jobs>();
	private HashMap<Integer, Jobs>checkedFutureJobs=new HashMap<Integer, Jobs>();
	private ArrayList<Parts> schift= new ArrayList<>();
	private ArrayList<Schift>splittedSchift= new ArrayList<Schift>();

	// list of assigned jobs to veh�cules _ option 1
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
		LinkedList<SubRoute> walkingRoutes=subroutes;
		this.coupleList=subJobsList2;
		if(subroutes!=null) {
			for(SubRoute wr:subroutes) {
				Jobs startNode=wr.getJobSequence().get(0);
				jobsInWalkingRoute.put(startNode.getId(), wr);
			}
		}
		rn=new Random(t.getSeed());
		RandomStreamBase stream = new LFSR113(); // L'Ecuyer stream
		t.setRandomStream(stream);
	}

	public void generateAfeasibleSolution() { 
		initialSol= createInitialSolution(); // la ruta ya deberia tener los arrival times
		System.out.println(initialSol.toString());
		solution= assigningRoutesToDrivers(initialSol);
	}





	//	private void settingFrequency() {
	//		for(Route r:solution.getRoutes()) {
	//			for(SubJobs j:r.getSubJobsList()) {
	//				frequency[j.get][]++;
	//			}	
	//		}
	//		
	//	}

	private Solution createInitialSolution() {
		// creationRoutes(); // create as many routes as there are vehicles
		// iteratively insert couples - here should be a destructive and constructive method 

		ArrayList<ArrayList<Couple>> clasification= clasificationjob(); // classification according to the job qualification
		// Los tiempos de las ventanas de tiempo son menores que la hora de inicio del servicio porque considera el tiempo del registro y el tiempo de carga y descarga del personal
		//settingStartServiceTime(); // late time - the start service time is fixed for the jobs which have a hard time window
		//	settingDeltas


		//		Solution sol1=assigmentJobsToVehicles(clasification);
		//		Solution sol2= creatingPoolRoute(sol1);
		//		if(sol1.getDurationSolution()<sol2.getDurationSolution()) {
		//			initialSol=new Solution(sol1);
		//		}
		//		else {
		//			initialSol=new Solution(sol2);
		//		}


		assigmentJobsToQualifications(clasification);
		//settingAssigmentSchift(clasification); // Create a large sequence of jobs-  the amount of sequences depende on the synchronization between time window each jobs - it does not consider the working hours of the personal- here is only considered the job qualification
		ArrayList<Route> route=insertingDepotConnections(schift);
		Solution assigmentPersonnalJob= solutionInformation(route); 
		savingInformationSchifts(assigmentPersonnalJob);
		assigmentPersonnalJob.checkingSolution(inp,test,jobsInWalkingRoute,assigmentPersonnalJob);
		System.out.println(assigmentPersonnalJob.toString());
		//initialSol.computeCosts(inp,test);

		//


		//		if(assigmentPersonnalJob.getDurationSolution()<initialSol.getDurationSolution()) {
		//			initialSol=new Solution(assigmentPersonnalJob);
		//		}
		//		System.out.println(initialSol.toString());
		//
		//
		//		double serviceTime=checkServiceTimes(initialSol);
		//		boolean areAllJobsAssigned=checkAssigment(initialSol);
		//		System.out.println(initialSol.toString());
		return new Solution(assigmentPersonnalJob);
	}



	private Solution assigmentVehicle(Solution assigmentPersonnalJob) {
		// 1. cambio de tiempos
		Solution sol= changingTimesVehicleRoute(assigmentPersonnalJob);
		mergingRoute(sol);
		return sol;
	}

	private void mergingRoute(Solution sol) { // merging shift <- medical staff
		ArrayList<Route> routesToMerge= new ArrayList<Route>();
		ArrayList<Route> newRoutes= new ArrayList<Route>();
		for(Route r:sol.getRoutes()) {
			if(r.getPartsRoute().size()>2) {
				routesToMerge.add(r);
			}
		}
		for(Route r:routesToMerge) {
			for(Route r1:routesToMerge) {

			}
		}

	}



	private Solution changingTimesVehicleRoute(Solution assigmentPersonnalJob) {
		// 1. copia de la soluci�n actual
		Solution currentSolution = new Solution (assigmentPersonnalJob);
		Solution newSolution = new Solution ();
		// 2. tratamiento de trabajos en cada ruta: definici�n de las ventanas de tiempo para cada trabajo hard
		for(Route r:currentSolution.getRoutes()) {
			treatmentRoute(r,newSolution);//changing TW
			System.out.println(r.toString());
		}

		return currentSolution;
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
			newRoute.getPartsRoute().add(pCopy);
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

		// actualizar information parts
		newRoute.updateRouteFromParts(inp, test, jobsInWalkingRoute);
		System.out.println("ref route");
		System.out.println(r.toString());
		System.out.println("new route");
		System.out.println(newRoute.toString());
		System.out.println("end");
	}

	private Solution creatingPoolRoute(Solution sol1) {
		Solution sol= new Solution();

		// PACIETES drop off
		// 1. Creaci�n de tantas rutas como trabajos se tienen 2. Cada trabajo se ubica en la mejor posici�n
		ArrayList<Parts> routesPool= generatingPoolRoutes();
		sol = selectingBestCombinationRoutes(routesPool,sol1);
		return sol;
	}

	private Solution selectingBestCombinationRoutes(ArrayList<Parts> routesPool, Solution sol1) {
		ArrayList<Route> route=insertingDepotConnections(routesPool);
		// creaci�n de partes
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
			if(xpressPoolRoutes.getDrivingRoutes().indexOf(r)==12) {
				System.out.println(r.toString());
			}
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
						System.out.println("Stop");
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

		System.out.println("routes Pool" + routesPool);
		System.out.println("routes Pool");	

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
			if(pickUp.getSubJobKey().equals("P32")) {
				System.out.println("Stop");
			}
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
		System.out.println("routes Pool" + routesPool);
		System.out.println("routes Pool");



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
			if(j.getSubJobKey().equals("P32")) {
				System.out.println(j.toString());
			}
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
					System.out.println("Stop");
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
				if(present.getSubJobKey().equals("P32")) {
					System.out.println("Stop");	
				}
				Parts newPart=new Parts();
				newPart.getListSubJobs().add(present);
				newPart.getListSubJobs().add(pickUp);
				newPart.getDirectorySubjobs().put(present.getSubJobKey(),present);
				newPart.getDirectorySubjobs().put(pickUp.getSubJobKey(),pickUp);
				pickUpDirectory.remove(pickUp.getSubJobKey());
				System.out.println("Stop");
				routesPool.add(newPart);
				//break;
			}
		}
		//newRoutes
		//	routesPool.add(newPart);
		System.out.println("routes Pool" + routesPool);
		System.out.println("routes Pool");

		// CLIENTES pick up

		for(SubJobs pickUp:listSubJobsPickUp) {
			if(pickUp.getSubJobKey().equals("P32")) {
				System.out.println("Stop");
			}
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
						System.out.println("Stop");
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
					if(pickUp.getSubJobKey().equals("P32")) {
						System.out.println("Stop");
					}
					Parts newPart=new Parts();
					newPart.getListSubJobs().add(pickUp);
					newPart.getDirectorySubjobs().put(pickUp.getSubJobKey(),pickUp);
					System.out.println("Stop");
					routesPool.add(newPart);
					//	break;

				}
			}
		}

		System.out.println("routes Pool" + routesPool);
		System.out.println("routes Pool");
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

	private void savingInformationSchifts(Solution initialSol2) {
		// conection infomration
		for(Route r:initialSol2.getRoutes()) {
			r.getSchiftRoute().setRouteList(r);
			// crear la connecci�n con el depot
			Edge e= new Edge (r.getPartsRoute().get(0).getListSubJobs().get(0),r.getSubJobsList().get(0),inp,test);
			r.getSchiftRoute().getConnections().add(e);
			for(int i=0;i<r.getSubJobsList().size()-1;i++) {
				SubJobs iSubJob=r.getSubJobsList().get(i);
				SubJobs jSubJob=r.getSubJobsList().get(i+1);
				originConnectionlist.put(jSubJob.getSubJobKey(), iSubJob);
				destinationConnectionlist.put(jSubJob.getSubJobKey(), jSubJob);
				e= new Edge (iSubJob,jSubJob,inp,test);
				r.getSchiftRoute().getConnections().add(e);
			}
			e= new Edge (r.getSubJobsList().get(r.getSubJobsList().size()-1),r.getPartsRoute().get(r.getPartsRoute().size()-1).getListSubJobs().get(0),inp,test);
			r.getSchiftRoute().getConnections().add(e);
		}


		//  each job belongs to a schift
		for(Route r:initialSol2.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				j.setShiftOwner(r);
			}
		}
		// setting frequency Route
		for(Route r:initialSol2.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				double [] penalizationCost= new double [initialSol2.getRoutes().size()];
				j.setFrecuencyRoute(penalizationCost);
			}
		}

	}

	private double checkServiceTimes(Solution initialSol2) {
		double service=0;
		for(Jobs j: inp.getNodes()) {
			service+=inp.getdirectoryNodes().get(j.getId());
		}
		return service;
	}

	private boolean checkAssigment(Solution initialSol2) {
		boolean check=false;
		HashMap<Integer, Jobs> missing= new HashMap<Integer, Jobs> ();
		for(Jobs j: inp.getNodes()) {
			if(j.getId()!=1) {
				missing.put(j.getId(), j);
			}
		}
		if(walkingRoutes!=null) {
			for(SubRoute r:walkingRoutes) {
				for(Jobs j:r.getJobSequence()) {
					if(j.getId()!=1) {
						missing.remove(j.getId());
					}
				}
			}
		}
		for(Route r:initialSol2.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				if(j.getId()!=1) {
					missing.remove(j.getId());
				}
			}
		}
		if(missing.isEmpty()) {
			check=true;
		}
		return check;
	}

	private Solution assigningRoutesToDrivers(Solution initialSol) {


		Solution startingSol=new Solution(initialSol);

		Solution newSol=null;

		Solution copySolution= assigmentVehicle(startingSol);// hasta aqu� algunas rutas pueden tener menos horas que las de la jornada laboral
		////

		for(int iter=0;iter<50;iter++) {
			
			Solution sol1= intraMergingParts0(copySolution);
			sol1.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
			System.out.println(sol1.getobjectiveFunction());
			if(sol1.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
				newSol=new Solution (sol1);
				startingSol=new Solution (sol1);
			}
			else {
				newSol=new Solution (startingSol);
			}

			Solution Sol2= interMergingParts(copySolution); // 1 merging parts (without complete parts)
			Sol2.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);

			if(Sol2.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
				newSol=new Solution (Sol2);
				startingSol=new Solution (Sol2);
			}
			else {
				newSol=new Solution (startingSol);
			}

			//			Solution sol0= treatment1(newSol);
			//			sol0.checkingSolution(inp,test,jobsInWalkingRoute);
			//			System.out.println(sol0.toString());
			//			
			//			if(sol0.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
			//				newSol=new Solution (sol0);
			//				startingSol=new Solution (sol0);
			//			}
			//			else {
			//				newSol=new Solution (startingSol);
			//			}

			//		Solution s1= shaking(newSol);
			//		System.out.println(s1.toString());
			//		s1.checkingSolution(inp,test,jobsInWalkingRoute);
			//		System.out.println(s1.toString());
			//
			//		if(s1.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
			//			newSol=new Solution (s1);
			//			startingSol=new Solution (s1);
			//		}

			//					Solution sol1= treatment2(startingSol);
			//					sol1.checkingSolution(inp,test,jobsInWalkingRoute);
			//					boolean areAllJobsAssigned=checkAssigment(sol1);
			//			
			//					
			//					if(sol1.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
			//						newSol=new Solution (sol1);
			//						startingSol=new Solution (sol1);
			//					}
			//					
			//					Solution sol2= treatment1(newSol);
			//					sol2.checkingSolution(inp,test,jobsInWalkingRoute);
			//					areAllJobsAssigned=checkAssigment(sol2);
			//					if(sol2.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
			//						newSol=new Solution (sol2);
			//						startingSol=new Solution (sol2);
			//					}
			//					

			//
			//								Solution alternativeSolution= assigmentTurnsToVehicles(newSol);
			//								alternativeSolution.checkingSolution(inp,test,jobsInWalkingRoute);
			//						
			//								if(alternativeSolution.getobjectiveFunction()<startingSol.getobjectiveFunction()) {
			//									newSol=new Solution (alternativeSolution);
			//									startingSol=new Solution (alternativeSolution);
			//								}
			copySolution= new Solution(newSol);
			//areAllJobsAssigned=checkAssigment(newSol);
		}
		return newSol;
	}









	//	private Solution treatment0(Solution initialSol2) {
	//		Solution copySolution= new Solution(initialSol2); // hasta aqu� algunas rutas pueden tener menos horas que las de la jornada laboral
	//       System.out.println(copySolution.toString());
	//		Solution newSolution= mergingRoutes0(copySolution); // las rutas se mezclan por partes
	//		System.out.println(newSolution.toString());
	//
	//		for(Route r:copySolution.getRoutes()) {
	//			for(int i=1;i<r.getPartsRoute().size()-1;i++) {
	//				Parts p= r.getPartsRoute().get(i);
	//				if(p.getDirectoryConnections().isEmpty()) {
	//					System.out.println("empty list of connections");
	//				}		
	//			}
	//		}
	//		boolean merge= checkingSubJobs(initialSol,newSolution);
	//		//Solution solFixedTime=insertTWNarrow(initialSol); // insertar la 
	//
	//		newSolution.checkingSolution(inp,test,jobsInWalkingRoute);
	//		//	newSolution.computeCosts(inp,test);
	//		return newSolution;
	//	}


	private HashMap<Integer, Jobs> clientInWalkingRoutes(WalkingRoutes subroutes2) {
		HashMap<Integer,Jobs> jobsInWalkingRoutes= new HashMap<Integer,Jobs>();
		if(subroutes2.getWalkingRoutes()!=null) {
			for(SubRoute r:subroutes2.getWalkingRoutes()) {
				if(r.getJobSequence().size()>1) {
					for(Jobs j:r.getJobSequence()) {
						jobsInWalkingRoutes.put(j.getId(), j); // storing the job in the walking route
					}
				}
			}
		}
		return jobsInWalkingRoutes;
	}

	void convertingWalkingRoutesInOneTask(ArrayList<Couple> coupleFromWalkingRoutes, WalkingRoutes subroutes2) {
		if(subroutes2.getWalkingRoutes()!=null) {
			for(SubRoute r:subroutes2.getWalkingRoutes()) {
				if(r.getDropOffNode()!=null && r.getPickUpNode()!=null) {
					double walkingRouteLength=r.getDurationWalkingRoute();

					// 0. creation of subjobs and fixing time windows 
					if(r.getDropOffNode().getId()==3) {
						System.out.println("couple ");
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
					System.out.println("couple ");
					System.out.println(pairPickUpDropOffHCS.toString());
					System.out.println("couple ");
					if(present.getstartServiceTime()>future.getstartServiceTime()) { // control
						System.out.println("error");
					}

				}
			}
		}
	}

	private void settingPeopleInSubJob(Couple pairPatientMedicalCentre, int i, int j) {
		pairPatientMedicalCentre.getPresent().setTotalPeople(i); // 1 persona porque se recoge s�lo al paciente
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
		future.setserviceTime(0); //los nodos pick up contienen la informaci�n de los nodos
		// Setting the TW
		double tv=inp.getCarCost().getCost(present.getId()-1, pickUpNode.getId()-1)*test.getDetour();
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


	private Solution mergingRoutes0(Solution copySolution) {
		// revisar la capacidad del veh�culo en tiempos de 
		Solution Sol= interMergingParts(copySolution); // 1 merging parts (without complete parts)
		System.out.println(Sol.toString());
		Sol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
		///Sol.computeCosts(inp,test);
		System.out.println(Sol.toString());
		settingNewPart(Sol);

		boolean merge= checkingSubJobs(Sol,copySolution);

		System.out.println("primer merging solution");


		Solution newSol= intraMergingParts0(copySolution);
		System.out.println(Sol.toString());
		newSol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
		System.out.println(Sol.toString());
		//newSol.computeCosts(inp,test);
		merge= checkingSubJobs(Sol,newSol);
		//Solution reducingRoutes= slackingTimes(newSol);

		System.out.println("second merging solution");
		System.out.println(newSol.toString());
		System.out.println("end");
		return newSol;
		//return Sol;
	}


	private Solution intraMergingParts0(Solution copySolution) {
		Solution newSol= new Solution(copySolution); // 1. copy solution
		if(copySolution.getRoutes().size()>=2) {
			// 2. Seleccionar las rutas que se van a mezclar
			ArrayList<Route> copyRoute = copyListRoute(copySolution); // copy original route for safe
			for(int i=0;i<copySolution.getRoutes().size()*2;i++) {
				int r1 = this.rn.nextInt(copySolution.getRoutes().size()-1);
				int r2 = this.rn.nextInt(copySolution.getRoutes().size()-1);

				Route refRoute=copySolution.getRoutes().get(r1);
				Route toSplit=copySolution.getRoutes().get(r2);
				if(toSplit.getJobsDirectory().containsKey("D6") || refRoute.getJobsDirectory().containsKey("D6")) {
					System.out.println("***Parte vacia");
				}
				boolean mergingParts= insertingPartIntoPart(refRoute,toSplit); // true <- cuando las rutas estan cambiando (se mezclan) false <-cuando las rutas permanecen igual

				System.out.println("***Parte vacia");

			}


			System.out.println("***Parte vacia");
			System.out.println(newSol.toString());
		}
		return newSol;
	}


	private void computingPenalization(Solution copySolution) {

		// 1. Waiting time
		for(Route r:copySolution.getRoutes()) {
			for(SubJobs s:r.getSubJobsList()) {
				if(s.getArrivalTime()+s.getloadUnloadRegistrationTime()+s.getloadUnloadTime()<s.getStartTime()) {
					double waitingTime=s.getStartTime()-(s.getArrivalTime()+s.getloadUnloadRegistrationTime()+s.getloadUnloadTime());
					s.setWaitingTime(waitingTime);
					if(waitingTime>test.getCumulativeWaitingTime()) {
						double additionalWaitingTime=test.getCumulativeWaitingTime()-waitingTime;
						s.setAdditionalWaitingTime(additionalWaitingTime);
					}
				}
			}
		}

		// 2. detour
	}

	private void computationFrequencyRoute(Solution copySolution) {
		for(Route r:copySolution.getRoutes()) {
			for(SubJobs s:r.getSubJobsList()) {
				s.getFrecuencyRoute()[r.getIdRoute()]+=1;
			}
		}	
	}



	private ArrayList<SubJobs> selectingHeadsSchifts(LinkedList<Route> linkedList) {
		ArrayList <SubJobs> heads = new ArrayList <SubJobs>();
		for(Route r:linkedList) {
			heads.add(r.getSubJobsList().get(0));
		}
		return heads;
	}

	private ArrayList<SubJobs> selectingJobsToAssign(Solution turnsPersonnel) {
		ArrayList<SubJobs> jobsToAssign = new ArrayList<SubJobs>();
		for(Route r:turnsPersonnel.getRoutes()) {
			for(int i=1;i<r.getPartsRoute().size();i++) {
				for(SubJobs s:r.getPartsRoute().get(i).getListSubJobs()) {
					jobsToAssign.add(s);
				}
			}	
		}

		return jobsToAssign;
	}

	private HashMap<String, SubJobs> listNodes(Solution turnsPersonnel) {
		HashMap <String, SubJobs> missing= new HashMap <>();
		for(Route r:turnsPersonnel.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				if(j.getId()!=1) {
					missing.put(j.getSubJobKey(), j);
				}
			}
		}
		return missing;
	}

	private Solution treatment1(Solution initialSol2) {
		Solution copySolution= new Solution(initialSol2); // hasta aqu� algunas rutas pueden tener menos horas que las de la jornada laboral
		Solution newSolution=new Solution(initialSol2);
		boolean merge=false;
		changingDepartureTimes(copySolution);
		if(!inp.getpatients().isEmpty()) {
			Solution patientsRoute=selectingPatientRoutes(copySolution);
			Solution presolpatientsRoute= mergingRoutes(patientsRoute); // las rutas se mezclan por partes
			merge= checkingSubJobs(patientsRoute,presolpatientsRoute);

			Solution presolhomeCareStaff=mergingTurnos(initialSol);
			Solution newInitialSolution=mergeSolutions(presolpatientsRoute,presolhomeCareStaff);
			newSolution= mergingRoutes(newInitialSolution); 
			newSolution.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
			//newSolution.computeCosts(inp,test);
			merge= checkingSubJobs(initialSol,newSolution);
		}
		else{
			newSolution= mergingRoutes(copySolution); // las rutas se mezclan por partes
			newSolution.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
			//newSolution.computeCosts(inp,test);
			merge= checkingSubJobs(initialSol,newSolution);
		}






		for(Route r:copySolution.getRoutes()) {
			for(int i=1;i<r.getPartsRoute().size()-1;i++) {
				Parts p= r.getPartsRoute().get(i);
				if(p.getDirectoryConnections().isEmpty()) {
					System.out.println("empty list of connections");
				}		
			}
		}
		merge= checkingSubJobs(initialSol,newSolution);
		//Solution solFixedTime=insertTWNarrow(initialSol); // insertar la 

		newSolution.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);


		return newSolution;
	}

	private Solution treatment2(Solution initialSol2) {
		Solution copySolution= new Solution(initialSol2); // hasta aqu� algunas rutas pueden tener menos horas que las de la jornada laboral

		Solution newSolution= mergingRoutes(copySolution); // las rutas se mezclan por partes



		for(Route r:copySolution.getRoutes()) {
			for(int i=1;i<r.getPartsRoute().size()-1;i++) {
				Parts p= r.getPartsRoute().get(i);
				if(p.getDirectoryConnections().isEmpty()) {
					System.out.println("empty list of connections");
				}		
			}
		}
		boolean merge= checkingSubJobs(initialSol,newSolution);
		//Solution solFixedTime=insertTWNarrow(initialSol); // insertar la 

		newSolution.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
		//	newSolution.computeCosts(inp,test);
		return newSolution;
	}

	private Solution mergeSolutions(Solution presolpatientsRoute, Solution presolhomeCareStaff) {
		Solution sol= new Solution ();
		for(Route r:presolpatientsRoute.getRoutes()) {
			if(!r.getSubJobsList().isEmpty()) {
				sol.getRoutes().add(r);
			}
		}
		for(Route r:presolhomeCareStaff.getRoutes()) {
			if(!r.getSubJobsList().isEmpty()) {
				sol.getRoutes().add(r);
			}
		}
		return sol;
	}


	private Solution selectingPatientRoutes(Solution copySolution) {
		Solution Sol= new Solution(copySolution);
		LinkedList<Route> routeToEliminate=selectingHCroute(Sol);
		for(Route r: routeToEliminate) {
			Sol.getRoutes().remove(r);	
		}

		Sol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
		//Sol.computeCosts(inp,test);
		return Sol;
	}

	private LinkedList<Route> selectingHCroute(Solution sol) {
		LinkedList<Route> routes= new LinkedList<Route>();
		for(Route r: sol.getRoutes()) {
			if(r.getAmountParamedic()==0 && r.getHomeCareStaff()!=0) {
				routes.add(r);
			}
		}
		return routes;
	}

	private Solution mergingTurnos(Solution copySolution) {
		Solution sol=new Solution();
		if(!inp.getclients().isEmpty()) {
			ArrayList<Route> walkingRoutes= new ArrayList<Route> ();
			ArrayList<Route> remainingRoutes= new ArrayList<Route> ();
			boolean isWalkingRotue=false;
			for(Route r:copySolution.getRoutes()) {
				isWalkingRotue=arewalkingRoutesIntheRotue(r); // true if at least one job represents a walking route
				if(isWalkingRotue) {
					isWalkingRotue=false;
					walkingRoutes.add(r);
				}
				else {
					if(r.getHomeCareStaff()>0) {
						remainingRoutes.add(r);}
				}
			}
			for(Route s: walkingRoutes) {
				boolean merge=mergingTwoRoutes(s,remainingRoutes);
				if(!merge) {
					merge=mergingTwoRoutes(s,walkingRoutes);
				}
			}
			for(Route s: remainingRoutes) {
				if(!s.getSubJobsList().isEmpty()) {
					boolean merge=mergingTwoRoutes(s,remainingRoutes);
				}
			}

			for(Route s: walkingRoutes) {
				if(!s.getSubJobsList().isEmpty()) {
					sol.getRoutes().add(s);
				}
			}

			for(Route s: remainingRoutes) {
				if(!s.getSubJobsList().isEmpty()) {
					sol.getRoutes().add(s);
				}
			}

			sol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
			//sol.computeCosts(inp,test);
		}
		return sol;
	}

	private boolean mergingTwoRoutes(Route s, ArrayList<Route> remainingRoutes) {
		boolean merge=false; // la idea es que la ruta de la walking route sea la ruta de referencia la rutas que se pueden eliminar son las que estan contenidas en remainingRoutes 
		if(s.getJobsDirectory().containsKey("P68")) {
			System.out.print("stop");
		}
		Route newRoute=new Route();
		double startTimeRoute=s.getPartsRoute().get(0).getListSubJobs().get(0).getArrivalTime();
		double endTimeRoute=s.getPartsRoute().get(s.getPartsRoute().size()-1).getListSubJobs().get(0).getArrivalTime();
		//if(endTimeRoute-startTimeRoute<test.getWorkingTime()) { // working hours
		for(Route potential: remainingRoutes) {
			if(!potential.getSubJobsList().isEmpty() && !potential.equals(s)) {
				if(potential.getJobsDirectory().containsKey("P13") && s.getJobsDirectory().containsKey("P35")) {
					System.out.print("stop");
				}
				merge=feasibilityMerging(potential,s,newRoute);
				printList(newRoute);

				if(merge) {
					settingParts(newRoute);
					s.getPartsRoute().clear();
					s.getSubJobsList().clear();
					for(Parts p:newRoute.getPartsRoute()) {
						s.getPartsRoute().add(p);	
					}
					updatingRefRoute(s,newRoute);
					// potential route
					updatingPotentialRout(s,potential);
					newRoute.getSubJobsList().clear();
					newRoute.getPartsRoute().clear();
					break;
				}
			}
		}
		//}
		System.out.print(s.toString());
		return merge;
	}

	private void updatingPotentialRout(Route s, Route potential) {
		ArrayList<SubJobs> jobs= new ArrayList<SubJobs>();
		for(SubJobs j:potential.getSubJobsList()) { // copy
			jobs.add(j);
		}
		for(SubJobs j:jobs) {
			if(s.getJobsDirectory().containsKey(j.getSubJobKey())){
				potential.getSubJobsList().remove(j);
			}
		}
		potential.getPartsRoute().clear();
		if(!potential.getSubJobsList().isEmpty()) {
			settingParts(potential);
			potential.updateRouteFromParts(inp,test,jobsInWalkingRoute);
			//earlyRoute.updateRouteFromSubJobsList(inp,test);
			for(Parts p:potential.getPartsRoute()) {
				p.settingConnections(p.getListSubJobs(),inp,test);
			}}
	}

	private void settingParts(Route newRoute) {
		// la creaci�n de estas rutas lo que hace es identificar las partes de cada turno
		// pero en esencia s�lo deberia agregar el deport
		System.out.println(newRoute.toString());
		//	calling depot
		//Jobs depot=inp.getNodes().get(0);
		SubJobs depotStart = new SubJobs(inp.getNodes().get(0));
		depotStart.setTotalPeople(1);
		SubJobs depotEnd = new SubJobs(inp.getNodes().get(0));
		depotEnd.setTotalPeople(1);
		ArrayList<SubJobs> partStart= new ArrayList<SubJobs>();
		ArrayList<SubJobs> partEnd= new ArrayList<SubJobs>();
		partStart.add(depotStart);

		partEnd.add(depotEnd);
		Parts partObject= new Parts();


		partObject.setListSubJobs(partStart,inp,test);
		newRoute.getPartsRoute().add(partObject);

		// 1. hacer las partes
		double passengers=1;
		ArrayList<SubJobs> part= new ArrayList<SubJobs>();
		part= new ArrayList<SubJobs>();
		partObject= new Parts();
		newRoute.getPartsRoute().add(partObject);
		for(int i=0;i<newRoute.getSubJobsList().size();i++) {
			SubJobs sj=newRoute.getSubJobsList().get(i);
			passengers+=sj.getTotalPeople();
			if(passengers!=0) {
				partObject.getListSubJobs().add(sj);
				if(i==newRoute.getSubJobsList().size()-1) {
					newRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
					System.out.println(newRoute.toString());
				}
			}
			else {
				partObject.getListSubJobs().add(sj);
				partObject= new Parts();
				newRoute.getPartsRoute().add(partObject);
				System.out.println(newRoute.toString());
			}		
		}
		partObject= new Parts();
		partObject.setListSubJobs(partEnd,inp,test);
		newRoute.getPartsRoute().add(partObject);
		newRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		// setting personnal
		if(newRoute.getSubJobsList().get(0).isPatient()) {
			newRoute.setAmountDriver(0);
			newRoute.setAmountParamedic(1);
			newRoute.setHomeCareStaff(0);
		}
		else {
			newRoute.setAmountDriver(0);
			newRoute.setAmountParamedic(0);
			newRoute.setHomeCareStaff(1);
		}
		System.out.println("Route");
		System.out.println(newRoute.toString());
		System.out.println("end");

		// setting starting and ending time

		SubJobs firstJob=newRoute.getSubJobsList().get(0); // it is not the depot node
		computeStartTimeRoute(firstJob,newRoute);
		SubJobs lastJob=newRoute.getSubJobsList().get(newRoute.getSubJobsList().size()-1);  // it is not the depot node
		computeEndTimeRoute(lastJob,newRoute);
		// setting connections
		for(int i=1;i<newRoute.getSubJobsList().size()-1;i++) {
			SubJobs previous=newRoute.getSubJobsList().get(i-1);
			SubJobs job=newRoute.getSubJobsList().get(i);
			Edge e=new Edge(previous,job,inp,test);
			newRoute.getEdges().put(e.getEdgeKey(), e);

		}
	}

	private boolean feasibilityMerging(Route potential, Route s, Route newRoute) {
		boolean inserted=false;
		newRoute=insertingWalkingJobs(s,newRoute);
		HashMap<String, SubJobs> subJobsID= selecctingMissingJobs(s,newRoute);
		ArrayList<SubJobs> toAssign=selectingJobsToAssign(potential,subJobsID);
		toAssign.sort(Jobs.SORT_BY_STARTW); 
		ArrayList<SubJobs> list= new ArrayList<SubJobs> ();
		for(SubJobs j:toAssign) { // copy
			list.add(j);
		}
		for(SubJobs j:list) {
			if(subJobsID.containsKey(j.getSubJobKey()) && j.isClient()) {
				inserted=false;
				if(j.isClient()) {
					SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
					SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);
					inserted=insertingIndividualJob(j1,newRoute, subJobsID);
					updatingCouple(j1,j2);
					if(inserted) {
						inserted=insertingIndividualJob(j2,newRoute,subJobsID);
					}
				}}

		}
		return inserted;
	}

	private ArrayList<SubJobs> selectingJobsToAssign(Route potential, HashMap<String, SubJobs> subJobsID) {
		ArrayList<SubJobs> jobsToAssign= new ArrayList<SubJobs>();
		for(SubJobs j:subJobsID.values()) {
			jobsToAssign.add(j);
		}
		for(SubJobs j:potential.getSubJobsList()) {
			jobsToAssign.add(j);
			subJobsID.put(j.getSubJobKey(), j);
		}
		return jobsToAssign;
	}

	private HashMap<String, SubJobs> selecctingMissingJobs(Route s, Route newRoute) {
		HashMap<String, SubJobs> subJobsID= new HashMap<String, SubJobs>();
		for(SubJobs j:s.getSubJobsList()){
			if(!newRoute.getJobsDirectory().containsKey(j.getSubJobKey())) {
				subJobsID.put(j.getSubJobKey(), j);
			}
		}
		return subJobsID;
	}

	private boolean insertingIndividualJob(SubJobs j1, Route newRoute, HashMap<String, SubJobs> subJobsID) {
		boolean inserted=false;
		if(subJobsID.containsKey(j1.getSubJobKey())) {
			inserted=insertingInTheRoute(newRoute,j1);	
		}
		if(inserted) {
			subJobsID.remove(j1.getSubJobKey());
		}
		return inserted;
	}

	private Route insertingWalkingJobs(Route s, Route newRoute) {
		for(SubJobs j:s.getSubJobsList()) { // assigning Walking Routes
			if(j.isClient()) {
				// s�lo voy a insertar los trabajos que son walking routes
				SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
				SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);
				if((j1.getEndTime()-j1.getStartTime())==0 && (j2.getEndTime()-j2.getStartTime())==0) {
					newRoute.getSubJobsList().add(j);
					newRoute.getJobsDirectory().put(j.getSubJobKey(), j);
				}
			}
		}
		return newRoute;
	}


	private void printList(Route r) {
		System.out.println("Route start");
		for(SubJobs j:r.getSubJobsList()) {
			System.out.println("ID "+j.getSubJobKey()+" B "+j.getstartServiceTime()+" TW["+j.getStartTime()+";"+j.getEndTime()+"]");
		}
		System.out.println("Route end");
	}

	private void assigmentSubJobsToVehicle(ArrayList<Route> routeSol, HashMap<String, SubJobs> subJobsID) {
		ArrayList<SubJobs> list= new ArrayList<SubJobs> ();
		for(SubJobs j:subJobsID.values()) {
			list.add(j);
		}
		for(SubJobs j:list) {
			if(j.getId()==9) {
				boolean a=false;
			}
			if(subJobsID.containsKey(j.getSubJobKey())) {
				boolean inserted=false;
				if(j.isClient()) {
					SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
					SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);
					inserted=insertingIndividualClientJob(j1,routeSol, subJobsID);
					updatingCouple(j1,j2);
					if(inserted) {
						inserted=insertingIndividualClientJob(j2,routeSol,subJobsID);
					}
				}}
		}

	}



	private void updatingCouple(SubJobs j1, SubJobs j2) {
		double arrivalTime=j1.getstartServiceTime()+j2.getReqTime(); // cuando puede pasar el veh�culo a recoger la enfermenra
		double departureTime=arrivalTime+test.getloadTimeHomeCareStaff(); // cuando puede pasar el veh�culo a recoger la enfermenra	
		j2.setStartTime(arrivalTime);// TW
		j2.setEndTime(arrivalTime);// TW
		j2.setarrivalTime(arrivalTime);
		j2.setStartServiceTime(arrivalTime);
		j2.setEndServiceTime(arrivalTime);
		j2.setdepartureTime(departureTime);
	}

	private boolean insertingIndividualClientJob(SubJobs j1, ArrayList<Route> routeSol, HashMap<String, SubJobs> subJobsID) {
		boolean inserted=false;
		if(subJobsID.containsKey(j1.getSubJobKey())) {
			for(Route r:routeSol) {
				inserted=insertingInTheRoute(r,j1);
				if(inserted) {
					break;
				}
			}
		}
		if(inserted) {
			subJobsID.remove(j1.getSubJobKey());
		}
		return inserted;
	}

	private boolean insertingInTheRoute(Route r, SubJobs j1) {
		boolean inserted=false;
		// iterando sobre la rute
		if(r.getSubJobsList().isEmpty()) { // en caso de que este vacia
			inserted=true;
			r.getSubJobsList().add(j1);
		}
		else {
			if(r.getSubJobsList().size()==1) { // en caso de que s�lo haya un trabajo
				SubJobs a=r.getSubJobsList().get(0);
				if(a.getDepartureTime()>j1.getstartServiceTime()) {
					inserted=true;
					r.getSubJobsList().add(0,j1);
					r.getJobsDirectory().put(j1.getSubJobKey(), j1);
				}
				else {
					inserted=true;
					r.getSubJobsList().add(j1);
					r.getJobsDirectory().put(j1.getSubJobKey(), j1);
				}
			}
			else { // m�s de un trabajo
				inserted=readingRouteAndChangingTime(j1,r);
			}
		}
		return inserted;
	}

	//	early=true;
	//	double startTime=Math.max(j1.getStartTime(),possibleStartTime);
	//	double ArrivalTime=startTime-test.getloadTimeHomeCareStaff();
	//	j1.setarrivalTime(ArrivalTime);
	//	j1.setStartServiceTime(startTime);
	//	j1.setEndServiceTime(startTime+j1.getReqTime());
	//	j1.setdepartureTime(j1.getendServiceTime());

	private boolean readingRouteAndChangingTime(SubJobs j1, Route r) {
		// checking if can be inserted with the current time
		boolean early=firstSubJob(j1,r.getSubJobsList().get(0),r.getSubJobsList());
		boolean late=false;
		boolean intermediateSubJob=false;
		boolean insertion=false;
		if(early) {

			r.getSubJobsList().add(0,j1);
			r.getJobsDirectory().put(j1.getSubJobKey(), j1);	
		} 
		else {
			late=lastSubJob(j1,r.getSubJobsList().get(r.getSubJobsList().size()-1),r.getSubJobsList());
			if(late) {
				r.getSubJobsList().add(j1);
				r.getJobsDirectory().put(j1.getSubJobKey(), j1);
			}
		}
		if(!late && !early) {
			// structure (a)---(j1)---(b)
			for(int j=0;j<r.getSubJobsList().size()-1;j++) {
				SubJobs a=r.getSubJobsList().get(j);
				SubJobs b=r.getSubJobsList().get(j+1);
				double tvAj1=inp.getCarCost().getCost(a.getId()-1, j1.getId()-1);
				double tvj1B=inp.getCarCost().getCost(j1.getId()-1,b.getId()-1);
				if(a.getDepartureTime()+tvAj1<=j1.getArrivalTime() && j1.getDepartureTime()+tvj1B<=b.getArrivalTime()) {
					ArrayList<SubJobs> copy= new ArrayList<SubJobs> ();
					for(SubJobs jsub:r.getSubJobsList()) {
						copy.add(jsub);
					}
					copy.add(j+1,j1);
					if(vehicleCapacityPart(copy)) {
						intermediateSubJob=true;
						r.getSubJobsList().add(j+1,j1);
						r.getJobsDirectory().put(j1.getSubJobKey(), j1);
						break;}
				}
				else {
					if((j1.getEndTime()-j1.getStartTime()>0)) {
						if(a.getEndTime()<j1.getEndTime() && j1.getEndTime()<b.getEndTime()) {// changing time
							double posibleDepartureTime=b.getArrivalTime()-tvj1B;
							double posibleStartTime=posibleDepartureTime-j1.getReqTime();
							if(a.getDepartureTime()<posibleStartTime && posibleStartTime>=j1.getStartTime() &&posibleStartTime<=j1.getEndTime() && posibleStartTime<b.getArrivalTime() ) {
								ArrayList<SubJobs> copy= new ArrayList<SubJobs> ();
								for(SubJobs jsub:r.getSubJobsList()) {
									copy.add(jsub);
								}
								copy.add(j+1,j1);
								if(vehicleCapacityPart(copy)) {
									intermediateSubJob=true;
									double startService=Math.max(j1.getStartTime(), posibleStartTime);
									double ArrivalTime=startService-test.getloadTimeHomeCareStaff();
									j1.setarrivalTime(ArrivalTime);
									j1.setStartServiceTime(startService);
									j1.setEndServiceTime(startService+j1.getReqTime());
									j1.setdepartureTime(j1.getendServiceTime());
								}
							}
						}
					}
				}
			}
		}
		if(early || intermediateSubJob || late) {
			insertion=true;
		}
		return insertion;
	}

	private boolean lastSubJob(SubJobs j1, SubJobs subJobs2, LinkedList<SubJobs> linkedList) {
		boolean late=false;
		double tv=inp.getCarCost().getCost(subJobs2.getId()-1, j1.getId()-1);
		if(subJobs2.getDepartureTime()+tv<=j1.getArrivalTime()) {
			ArrayList<SubJobs> copy= new ArrayList<SubJobs> ();
			for(SubJobs j:linkedList) {
				copy.add(j);
			}
			copy.add(0,j1);
			if(vehicleCapacityPart(copy)) {
				late=true;}
		}	
		return late;
	}

	private boolean firstSubJob(SubJobs j1, SubJobs s, LinkedList<SubJobs> linkedList) {
		boolean early=false;
		double tv=inp.getCarCost().getCost(j1.getId()-1, s.getId()-1);
		if(j1.getDepartureTime()+tv<=s.getArrivalTime()) {	
			ArrayList<SubJobs> copy= new ArrayList<SubJobs> ();
			for(SubJobs j:linkedList) {
				copy.add(j);
			}
			copy.add(0,j1);
			if(vehicleCapacityPart(copy)) {
				early=true;
			}

		}	
		else { // changing time
			if((j1.getEndTime()-j1.getStartTime())>0) {
				double possibleDeparture=s.getArrivalTime()-tv;
				double possibleStartTime=possibleDeparture-j1.getReqTime();
				if(j1.getStartTime()<=possibleStartTime &&j1.getEndTime()>=possibleStartTime && s.getArrivalTime()>possibleStartTime) {
					ArrayList<SubJobs> copy= new ArrayList<SubJobs> ();
					for(SubJobs j:linkedList) {
						copy.add(j);
					}
					copy.add(0,j1);
					if(vehicleCapacityPart(copy)) {
						early=true;
						double startTime=Math.max(j1.getStartTime(),possibleStartTime);
						//double startTime=Math.max(j1.getStartTime(),possibleStartTime);
						double ArrivalTime=startTime-test.getloadTimeHomeCareStaff();
						j1.setarrivalTime(ArrivalTime);
						j1.setStartServiceTime(startTime);
						j1.setEndServiceTime(startTime+j1.getReqTime());
						j1.setdepartureTime(j1.getendServiceTime());}
				}
			}
		}	
		return early;
	}

	private HashMap<String, SubJobs> poolSubJobsToAssign(ArrayList<SubJobs> subJobsToAssign, ArrayList<Route> remainingRoutes) {
		HashMap<String, SubJobs> list= new HashMap<String, SubJobs>();
		for(SubJobs j:subJobsToAssign) {
			list.put(j.getSubJobKey(), j);
		}
		for(Route r: remainingRoutes) {
			for(SubJobs j:r.getSubJobsList()) {
				subJobsToAssign.add(j);
				list.put(j.getSubJobKey(), j);
			}
		}
		return list;
	}


	private boolean arewalkingRoutesIntheRotue(Route r) {
		boolean isWalkingRotue=false;
		for(SubJobs j:r.getSubJobsList()) {
			if(j.isClient()) {
				SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
				SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);
				if(j1.getEndTime()-j1.getStartTime()==0 && j2.getEndTime()-j2.getStartTime()==0) {
					isWalkingRotue=true;
					break;
				}
			}
		}
		return isWalkingRotue;
	}

	private ArrayList<Route> assigmentWalkingRouteToVehicle(ArrayList<Route> walkingRoutes) {
		ArrayList<Route> routeSol=new ArrayList<Route>();
		for(int i=0;i<inp.getVehicles().get(0).getQuantity();i++) {
			routeSol.add(new Route());
		}
		for(Route r:walkingRoutes) {// toda ruta que contenga al menos una walking route se selecciona para ser asignada
			assignningBigJobs(r,routeSol);
		}
		return routeSol;
	}

	private void assignningBigJobs(Route r, ArrayList<Route> routeSol) {
		//
		boolean insertion=false;
		HashMap<String,SubJobs> listJobs= new  HashMap<String,SubJobs>();
		LinkedList<SubJobs> jobs= new LinkedList<SubJobs>();
		for(SubJobs j:r.getSubJobsList()) {
			jobs.add(j);
			listJobs.put(j.getSubJobKey(), j);
		}

		for(SubJobs j:jobs) {
			if(listJobs.containsKey(j.getSubJobKey())) {
				if((j.getEndTime()-j.getStartTime())==0) {
					insertion=assignWalkingRouteVehicle(listJobs,j,routeSol);
				}
			}
		}
		if(insertion) {
			for(SubJobs j:jobs) {
				r.getSubJobsList().remove(j);
			}
		}
	}

	private boolean assignWalkingRouteVehicle(HashMap<String, SubJobs> listJobs, SubJobs j, ArrayList<Route> routeSol) {
		// se inserta sea como sea los dos subJobs asociados a j
		boolean inserted=false; // true cuando se insertan las dos partes
		SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
		SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);

		for(Route route:routeSol) {// se asigna a una ruta
			inserted=assigningJob(j1,route,listJobs);
			if(inserted){
				// se inserta la segunda parte
				inserted=assigningJob(j2,route,listJobs);
			}
			if(inserted) {
				break;
			}
		}
		if(!inserted) { // se creo una nueva ruta
			Route r=new Route();
			if(listJobs.containsKey(j1.getSubJobKey())) {
				r.getSubJobsList().add(j1);
				listJobs.remove(j1.getSubJobKey());
			}
			if(listJobs.containsKey(j2.getSubJobKey())) {
				r.getSubJobsList().add(j2);
				listJobs.remove(j2.getSubJobKey());
			}
			routeSol.add(r);
		}
		return inserted;
	}

	private boolean assigningJob(SubJobs j1, Route route, HashMap<String, SubJobs> listJobs) {
		boolean inserted=false;
		if(listJobs.containsKey(j1.getSubJobKey())) {
			if(route.getSubJobsList().isEmpty()) {
				inserted=true;
				route.getSubJobsList().add(j1);	
			}
			else {
				inserted=readingSubJobList(route,j1);
			}
		}
		if(inserted) {
			listJobs.remove(j1.getSubJobKey());
		}
		return inserted;
	}

	private boolean readingSubJobList(Route route, SubJobs j1) {
		// structure (a)---(j1)---(b)
		boolean inserted=false;
		for(int i=0;i<route.getSubJobsList().size()-1;i++){
			SubJobs a=route.getSubJobsList().get(i);

			SubJobs b=route.getSubJobsList().get(i+1);
			if(a.getDepartureTime()<j1.getArrivalTime() && j1.getDepartureTime()<b.getArrivalTime()) {
				inserted=true;
				route.getSubJobsList().add(i+1,j1);
			}

		}

		if(route.getSubJobsList().size()==1) {
			SubJobs a=route.getSubJobsList().get(0);
			// si s�lo hay un trabajo
			if(a.getstartServiceTime()>j1.getstartServiceTime()) {
				inserted=true;
				route.getSubJobsList().add(0,j1);
			}
			else {
				inserted=true;
				route.getSubJobsList().add(j1);
			}
		}
		return inserted;
	}

	private boolean checkDoubleJobs(Solution newSol) {
		boolean uniqueJobs=true;
		HashMap<String, SubJobs> storeJobs= new HashMap<>();
		for(Route r:newSol.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				if(j.getId()!=1) {
					if(!storeJobs.containsKey(j.getSubJobKey())) {
						storeJobs.put(j.getSubJobKey(), j);
					}
					else {
						uniqueJobs=false; 
						break;
					}
				}
			}
			if(!uniqueJobs) {
				break;
			}
		}
		return uniqueJobs;
	}

	private Solution sclackTime(Solution newSol) {
		// compute: arrival time, start service time, end service time, departure time
		Solution copySolution= new Solution(initialSol);
		for(Route r:copySolution.getRoutes()) {
			int routeLength=r.getSubJobsList().size()-1;
			for(int i=routeLength;i>0;i--) {
				// Last subjob
				SubJobs jSubJob=r.getSubJobsList().get(i);

				// additional time
				double additionalTime=0;
				if(jSubJob.isClient()) {
					additionalTime=test.getloadTimeHomeCareStaff();
				}
				else {
					if(jSubJob.getId()!=1) {
						additionalTime=test.getloadTimeHomeCareStaff();	
					}
				}

				jSubJob.setarrivalTime(jSubJob.getstartServiceTime()-additionalTime);
				jSubJob.setdepartureTime(jSubJob.getstartServiceTime()+additionalTime);
				// previous to the last subjob
				SubJobs iSubJob=r.getSubJobsList().get(i-1);
				double tv=inp.getCarCost().getCost(iSubJob.getId()-1, jSubJob.getId()-1);
				double newDepartureTime=jSubJob.getArrivalTime()-tv; // el veh�culo parte del nodo
				if(newDepartureTime<=iSubJob.getEndTime() && newDepartureTime>= iSubJob.getStartTime()) {
					iSubJob.setStartServiceTime(newDepartureTime);
					iSubJob.setEndServiceTime(iSubJob.getstartServiceTime()+iSubJob.getReqTime());
				}
			}
			r.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		}

		return copySolution;
	}

	private boolean checkingSubJobs(Solution refSolution,Solution test) {
		boolean merge=false;
		for(Route r:refSolution.getRoutes()) {
			for(SubJobs s:r.getSubJobsList()){
				if(s.getId()!=1) {
					assignedSubJobs.put(s.getSubJobKey(), s);
				}
			}
		}

		HashMap<String, SubJobs>list= new HashMap<String, SubJobs>();
		for(Route route:test.getRoutes()){
			for(SubJobs s:route.getSubJobsList()) {
				list.put(s.getSubJobKey(), s);
			}

		}
		for(SubJobs ss:assignedSubJobs.values()) {
			if(list.containsKey(ss.getSubJobKey())) {
				list.remove(ss.getSubJobKey());
			}
			else {missingubJobs.put(ss.getSubJobKey(), ss);}
		}
		if(missingubJobs.isEmpty()){
			merge=true;	
		}

		return merge;
	}

	private void changingDepartureTimes(Solution copySolution) {
		// se cambian los departure times de cada subjob en cada parte
		for(Route r:copySolution.getRoutes()) {
			for(Parts a:r.getPartsRoute()) {
				changinPartTime(a);
			}	
			r.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		}

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

	private LinkedList<Parts> extractingParts(Solution initialSol2) {
		LinkedList<Parts> partsList= new LinkedList<>();
		for(Route r:initialSol2.getRoutes()) {
			for(Parts part:r.getPartsRoute()) {
				partsList.add(part);
			}
		}
		return partsList;
	}

	private Solution mergingRoutes(Solution copySolution) {
		// revisar la capacidad del veh�culo en tiempos de 
		Solution Sol= interMergingParts(copySolution); // 1 merging parts (without complete parts)
		Sol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);
		//Sol.computeCosts(inp,test);

		settingNewPart(Sol);

		boolean merge= checkingSubJobs(Sol,copySolution);

		System.out.println("primer merging solution");
		System.out.println(Sol.toString());


		Solution newSol= intraMergingParts(Sol); // 2 merging parts (splited the parts) // here the detours are considered

		newSol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);

		//newSol.computeCosts(inp,test);
		merge= checkingSubJobs(Sol,newSol);
		//Solution reducingRoutes= slackingTimes(newSol);

		System.out.println("second merging solution");
		System.out.println(newSol.toString());
		System.out.println("end");
		return newSol;
	}



	private Route callingRoutePair(Solution newSol, SubJobs s) {
		Route r=null;
		Couple n=coupleList.get(s.getSubJobKey());
		SubJobs mainElement=null;
		if(n.getPresent().getSubJobKey().equals(s.getSubJobKey())) {
			mainElement=(SubJobs)n.getFuture();
		}
		else {
			mainElement=(SubJobs)n.getFuture();
		}
		for(Route route:newSol.getRoutes()) {
			if(route.getJobsDirectory().containsKey(mainElement.getSubJobKey())) {
				if(mainElement.getEndTime()-mainElement.getStartTime()>0) {
					r=route;
					break;
				}
				else {
					break;
				}
			}
		}
		return r;
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
					System.out.println("***Parte vacia");
				}
				boolean mergingParts= insertingPartIntoPart(refRoute,toSplit); // true <- cuando las rutas estan cambiando (se mezclan) false <-cuando las rutas permanecen igual
				System.out.println("***Parte vacia");
			}
		}


		System.out.println("***Parte vacia");
		System.out.println(newSol.toString());

		return newSol;
	}

	private boolean insertingPartIntoPart(Route refRoute, Route toSplit) {
		boolean merging=false;
		boolean mergingParts=false;
		if((refRoute.getDurationRoute()+toSplit.getDurationRoute())<=test.getRouteLenght()) {
			//if((refRoute.getTravelTime()+refRoute.getloadUnloadRegistrationTime()+toSplit.getTravelTime()+toSplit.getloadUnloadRegistrationTime())<test.getRouteLenght()) {
			mergingParts=true;
		}
		if(refRoute!=toSplit && mergingParts) { // s�lo se pueden mezclar si son rutas diferentes
			// se tiene que tener cuidado con las working horas y con los detours

			if(refRoute.getIdRoute()==7 && toSplit.getIdRoute()==4) {
				System.out.println("Stop");
			}
			boolean node=false;
			Route earlyRoute=selecctingStartRoute(refRoute,toSplit);
			Route lateRoute=selecctingRouteToInsert(refRoute,toSplit);
			if(earlyRoute==refRoute) {
				Route newRoute= new Route(); // la ruta que va a almacenar 

				// 1. Evaluar mezclar las primeras partes 
				boolean mergingHeading=seatingHeading(earlyRoute,lateRoute,newRoute);
				// 2. Evaluar mezclar las partes intermedias
				if(mergingHeading ) {
					System.out.println("Stop");
				}

				boolean mergingIntermediateParts=settingIntermediateParts(earlyRoute,lateRoute,newRoute);
				if(mergingIntermediateParts) {
					System.out.println("Stop");
				}

				// 3. Evaluar mezclar las ultimas partes
				//TO DO: boolean mergingTail=settingTailsParts(earlyRoute,lateRoute,mergingPart);
				// 4. Los trabajos contenidos en la nueva ruta corresponden a los nuevos trabajos que va a integrar la ruta m�s temprana
				boolean mergingLastParts=settingTailsParts(earlyRoute,lateRoute,newRoute);
				if(mergingLastParts) {
					System.out.println("Stop");
				}
				updatingRefRoute(earlyRoute,newRoute);
				updatingLateRoute(lateRoute,earlyRoute,newRoute); // los trabajos que aparecen en la ruta nueva no tienen porque aparecer en la ruta vieja
				System.out.println("Stop");
				System.out.println("Route earlyRoute"  +earlyRoute.toString());
				System.out.println("Route Late"  +lateRoute.toString());
				System.out.println("Stop");}
		}
		return merging;
	}

	private boolean settingTailsParts(Route earlyRoute, Route lateRoute, Route newRoute) {
		boolean merge =false;
		ArrayList<Parts> partsList=selectingParts(earlyRoute,lateRoute);// 0<- parte m�s larga 1<- parte m�s corta
		System.out.println(earlyRoute.toString());
		System.out.println(lateRoute.toString());
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
	//		ArrayList<Parts> partsList=selectingParts(earlyRoute,lateRoute);// 0<- parte m�s larga 1<- parte m�s corta
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
				double remainingDistanceshortPart=remainingDistance(-1,partsList);// el toInsert es m�s tarde que el primer trabajo en la parte larga
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
		for(SubJobs s: list) {
			action=s.getTotalPeople();
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
		System.out.println("total HHC"+ homeCareStaff);
		System.out.println("total Paramedic"+ paramedic);
		System.out.println("total");


		return enoughCapacity;
	}



	private boolean earlyInsertion(SubJobs toInsert, Edge edgeShortPart, ArrayList<Parts> partsList) {
		boolean merge=false;
		if(toInsert.getDepartureTime()<partsList.get(0).getListSubJobs().get(0).getDepartureTime() && toInsert.getDepartureTime()<partsList.get(0).getListSubJobs().get(0).getArrivalTime()) {
			double remainingDistanceshortPart=remainingDistance(-1,partsList);// el toInsert es m�s tarde que el primer trabajo en la parte larga
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
				System.out.println("stop");
			}
			p.settingConnections(p.getListSubJobs(),inp,test);
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
		// este m�todo no esta definido. Esta provisional
		// 1. Es necesario indetificar el tipo de partes a mezclar

		boolean merging=false;
		// empieza a mezclar desde la parte 2 de las rutas
		for(int partEarly=2; partEarly<(earlyRoute.getPartsRoute().size()-2);partEarly++) {
			System.out.print("Size"+ (lateRoute.getPartsRoute().size()-1));
			for(int partLate=2; partLate<(lateRoute.getPartsRoute().size()-2);partLate++) {
				System.out.print("Current partt"+ partLate);
				if(lateRoute.getIdRoute()==5 && earlyRoute.getIdRoute()==0 && partEarly==2 && partLate==4 ) {
					System.out.println("early \n");
				}
				if(partEarly==4 && partLate==2 ) {
					System.out.println("early \n");
				}
				int options=typeOfParts(earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate)); 

				switch(options) {
				case 1 :// 1 patient-patient 
					System.out.println("early ID\n"+earlyRoute.getIdRoute());
					System.out.println("early \n"+earlyRoute);
					System.out.println("late ID\n"+lateRoute.getIdRoute());
					System.out.println("late \n"+lateRoute);
					if(lateRoute.getIdRoute()==5 && earlyRoute.getIdRoute()==0 && partEarly==2 && partLate==4 ) {
						System.out.println("early \n");
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
	//		// este m�todo no esta definido. Esta provisional
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
				// evaluate option 2: en la opci�n dos no se consideran los detours
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
			// evaluate option 2: en la opci�n dos no se consideran los detours
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
				// evaluate option 2: en la opci�n dos no se consideran los detours
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
		ArrayList<Parts> ordering=orderingParts(earlyPart,latePart); //ordena de manera descendente las partes de acuerdo al n�mero de subjobs contenidos en cada parte 
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
					System.out.println("stop");
				}
				double detourEarly=pickUpEarlypickUpLate+inp.getCarCost().getCost(pickUpLate.getId()-1, dropOffEarly.getId()-1);

				// control detour early job (pickUpPatientLate)---x---(dropOffPatientLate)
				key=pickUpLate.getSubJobKey()+dropOffLate.getSubJobKey();
				Edge connectionLate=latePart.getDirectoryConnections().get(key);
				if(connectionLate==null) {
					System.out.println("stop");
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
		boolean headingMerge= false;// la early es la ruta de referencia  - eso quiere decir que al final el array mergingPart debe ser igual a la ruta m�s cercana
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
	//		// este m�todo lo que hace es modificar la ruta temprana la earlyRoute
	//		boolean feasibleOption1=false;
	//
	//		mergingPart(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),newRoute,feasibleOption1);
	//
	//		return feasibleOption1;
	//	}


	private boolean mergingHeading(Route earlyRoute, Route lateRoute, Route newRoute) {
		// este m�todo lo que hace es modificar la ruta temprana la earlyRoute
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

	private boolean changeTimeClients(int options, Parts early, Parts late, Route newRoute) {
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
		// changing times

		double loadUnloadTime=0;
		double registration=0;
		if(lateDropOff.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=earlyDropOff.getDepartureTime()-distEarlyDropOffLateDropOff;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		if(startServiceTime>=lateDropOff.getStartTime()  && startServiceTime<=lateDropOff.getEndTime()) {
			feasible=true;
			listSubJobs.add(earlyDropOff);
			listSubJobs.add(lateDropOff);
			newPart.setListSubJobs(listSubJobs, inp, test);
			newPart.settingConnections(early,late);
			newRoute.getPartsRoute().add(newPart);
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
		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opci�n en que el prime trabajo sea uno asociado a un home care staff 
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
		else {// opci�n en que el primer trabajo se uno asociado a  un paciente
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

		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opci�n en que el prime trabajo sea uno asociado a un home care staff 
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
		else {// opci�n en que el primer trabajo se uno asociado a  un paciente
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

		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opci�n en que el prime trabajo sea uno asociado a un home care staff 
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
		else {// opci�n en que el primer trabajo se uno asociado a  un paciente
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
				boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);// connecci�n con el depot y connecci�n entre trabajos
				boolean detourEarlySubjob=checkingDetourEarlyPatient(disHomeMedicalCentreEarly,PatientHouseMedicalCentreEarly);// connecci�n con el depot y connecci�n entre trabajos
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
						boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);// connecci�n con el depot y connecci�n entre trabajos
						boolean detourEarlySubjob=checkingDetourEarlyPatient(disHomeMedicalCentreEarly,PatientHouseMedicalCentreEarly);// connecci�n con el depot y connecci�n entre trabajos
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
		// son s�lo dos clientes
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
		int part1=parts.getListSubJobs().size();
		int part2=parts2.getListSubJobs().size();
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

	private boolean mergingPart(Parts pref, Parts psplit,ArrayList<SubJobs> mergingPart) {
		// pref <- early part // psplit<- late part
		boolean mergingParts= false;





		for(int i=0;i<pref.getListSubJobs().size();i++) {
			SubJobs j0=pref.getListSubJobs().get(i);
			for(int j=0;j<pref.getListSubJobs().size();j++) {
				SubJobs j1=pref.getListSubJobs().get(j);
				boolean feasibleDetour=respectMaxdetour(i,j,pref,psplit);
				boolean insertingAfterJo= insertingSubjobs(j0,j1,pref,psplit);

				if(j0.getArrivalTime()<j1.getArrivalTime()) {

				}

			}
		}


		SubJobs lastInsertedJob=mergingPart.get(mergingPart.size()-1);
		SubJobs firstJobEarly=psplit.getListSubJobs().get(0);
		SubJobs firstJobLate=psplit.getListSubJobs().get(0);
		if(firstJobLate.getId()!=1) {
			if(firstJobEarly.getArrivalTime()<firstJobLate.getArrivalTime()) { // es factible ir del primer trabajo de pref a el primer trabajo psplit
				double tv=inp.getCarCost().getCost(firstJobEarly.getId()-1, firstJobLate.getId()-1); 
				double preliminarArrivalTime=firstJobEarly.getDepartureTime()+tv;
				if(firstJobLate.getArrivalTime()>preliminarArrivalTime) {
					if(firstJobLate.getArrivalTime()>preliminarArrivalTime) {


					}

				}
			}

		}
		return mergingParts;
	}


	private boolean respectMaxdetour(int i, int j, Parts pref, Parts psplit) {
		boolean feasible=false;
		if(i>0) { // evaluar el detour para el trabajo m�s cercano
			SubJobs jobN=pref.getListSubJobs().get(i);
			SubJobs jobO=pref.getListSubJobs().get(i);
			SubJobs jobP=pref.getListSubJobs().get(i);
		}

		return feasible;
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
	//		// tengo que asegurar que la jornada laboral destinada al conductor sea mayor que la m�xima jornada permitida
	//		Solution copySolution=new Solution(sol);
	//		System.out.println(copySolution.toString());
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
	//						System.out.println("Route refRoute"+ refRoute.toString());
	//						System.out.println("Route toInsertRoute"+ toInsertRoute.toString());
	//						System.out.println("Route toInsertRoute");
	//						//routeVehicleList.add(vehicle);
	//					}
	//				}
	//				else {
	//					break;
	//				}
	//			}
	//		}
	//		System.out.println(copySolution.toString());
	//		settingSolution(copySolution);
	//
	//		return copySolution;
	//	}

	private Solution interMergingParts(Solution sol) {
		// tengo que asegurar que la jornada laboral destinada al conductor sea mayor que la m�xima jornada permitida
		Solution copySolution=new Solution(sol);
		System.out.println(copySolution.toString());
		for(int route1=0;route1<copySolution.getRoutes().size();route1++) {
			for(int route2=0;route2<copySolution.getRoutes().size();route2++) {
				int part=0;
				int start=2;
				Route vehicle= new Route();
				Route iR=copySolution.getRoutes().get(route1);
				if(iR.getPartsRoute().size()>2) {
					Route jR=copySolution.getRoutes().get(route2);
					//					System.out.println("\nRoute iR"+ iR.toString());
					//					System.out.println("\nRoute jR"+ jR.toString());
					if(possibleMerge(iR,jR)) {
						if(iR.getJobsDirectory().containsKey("D70") || jR.getJobsDirectory().containsKey("D70")) {
							System.out.println("\nRoute iR"+ iR.toString());
							System.out.println("\nRoute jR"+ jR.toString());
						}
						//						if(iR.getJobsDirectory().containsKey("P6") || jR.getJobsDirectory().containsKey("P6")) {
						//							System.out.println("\nRoute iR"+ iR.toString());
						//							System.out.println("\nRoute jR"+ jR.toString());
						//						}
						Route refRoute=selecctingStartRoute(iR,jR);
						Route toInsertRoute=selecctingRouteToInsert(iR,jR);
						//						if(refRoute.getIdRoute()==1 && toInsertRoute.getIdRoute()==22) {
						//							System.out.println("\nRoute refRoute"+ refRoute.toString());
						//							System.out.println("\nRoute toInsertRoute"+ toInsertRoute.toString());
						//						}
						//						System.out.println("\nRoute refRoute"+ refRoute.toString());
						//						System.out.println("\nRoute toInsertRoute"+ toInsertRoute.toString());
						//						if(refRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==21 && toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==7) {
						//							System.out.println("\nRoute iR"+ iR.toString());
						//							System.out.println("\nRoute jR"+ jR.toString());
						//						}
						if(toInsertRoute.getIdRoute()==5 || refRoute.getIdRoute()==5) {
							System.out.println("\nRoute toInsertRoute"+ toInsertRoute.toString());
							System.out.println("\nRoute refRoute"+ refRoute.toString());
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
									part=newStart; // para que siga metiendo las partes de la ruta que a�n falta por incorporar
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

						System.out.println("Route refRoute"+ refRoute.toString());
						System.out.println("Route toInsertRoute"+ toInsertRoute.toString());
						System.out.println("Route toInsertRoute");
						//routeVehicleList.add(vehicle);
					}
				}
				else {
					break;
				}
			}
			//updatingListRoutes();
			//			System.out.println(" Route list ");
			//			for(Route r:routeVehicleList) {
			//				System.out.println(r.toString());
			//			}
		}
		System.out.println(copySolution.toString());
		settingSolution(copySolution);

		return copySolution;
	}

	private void totalPersonalFromDepot(Route r) {
		double amountParamedic=0;
		double amountHomeCareStaff=0;
		double dropOffHomeCare=0;
		double pickUppatient=0;

		for(SubJobs j:r.getSubJobsList()) {
			if(j.isClient()) {
				dropOffHomeCare+=j.getTotalPeople();
				if(amountHomeCareStaff<Math.abs(dropOffHomeCare)) {
					amountHomeCareStaff++;
				}
			}
			if(j.isPatient() ) {
				pickUppatient+=j.getTotalPeople();
				if(amountParamedic<Math.abs(pickUppatient)) {
					amountParamedic++;
				}
			}
		}
		r.setAmountParamedic(amountParamedic);
		r.setHomeCareStaff(amountHomeCareStaff);
		r.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople((int)(amountParamedic+amountHomeCareStaff));
	}

	private ArrayList<SubJobs> selectingSubJobs(Route iR, Route jR) {
		ArrayList<SubJobs> list = new ArrayList<SubJobs> ();
		for(SubJobs j:iR.getSubJobsList()) {
			list.add(j);
		}
		for(SubJobs j:jR.getSubJobsList()) {
			list.add(j);
		}
		list.sort(Jobs.SORT_BY_ENDTW);
		return list;
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

	private boolean changetheTimes(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		// estructure: lastSubjobInRoute ----firstSubjob**---
		boolean inserted=false;
		int lastPart=refRoute.getPartsRoute().size()-1;// <- esta parte hace referencia al depot
		//	.getListSubJobs().get(0);
		Parts partEnd=refRoute.getPartsRoute().get(lastPart-1);
		SubJobs lastSubjobInRoute= partEnd.getListSubJobs().get(partEnd.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
		// computing new Times
		double loadUnloadTime=0;
		double registration=0;
		if(firstSubjob.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=lastSubjobInRoute.getDepartureTime()-tv;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		if(startServiceTime >=firstSubjob.getStartTime() && startServiceTime<=firstSubjob.getEndTime()) {
			firstSubjob.setarrivalTime(arrival);
			firstSubjob.setStartServiceTime(startServiceTime);
			firstSubjob.setEndServiceTime(startServiceTime+firstSubjob.getReqTime());
			firstSubjob.setdepartureTime(departure);
		}

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
		// el objetivo es dejar que la ruta refRoute s�lo tenga partes contenidas en la ruta de vehicle
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
						System.out.println("Printing changing 2 " +vehicle.toString());
						changing.removingParts(toInsertRoute.getPartsRoute().get(i));
						changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
						System.out.println("Printing changing 1 " +changing.toString());
						System.out.println("Printing vehicleRouting" +vehicle.toString());
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
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted;
	}

	private boolean changeServiceTime(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		for(int i=1;i<toInsertRoute.getPartsRoute().size()-1;i++) { // -1 para no incluir el depot al final
			// primer intenta insertar la parte de la otra ruta
			// structure route ref part 1 {1,...,lastSubjobInRoute} part 2{nextSubJobPart, } route to insert part 1 {firstSubjob,...,lastSubjob}
			Parts endPart=vehicle.getPartsRoute().getLast();
			SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
			SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(0);
			SubJobs lastSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(toInsertRoute.getPartsRoute().get(i).getListSubJobs().size()-1);
			SubJobs nextSubJobPart= refRoute.getPartsRoute().get(part2).getListSubJobs().get(0);
			double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
			boolean nextPart= checkingNextPart(lastSubjobInRoute,firstSubjob,lastSubjob,nextSubJobPart,refRoute.getPartsRoute().get(part2),toInsertRoute.getPartsRoute().get(i),vehicle);
			if((lastSubjobInRoute.getDepartureTime()+tv)<firstSubjob.getArrivalTime() && nextPart) {
				if((firstSubjob.getArrivalTime()-(lastSubjobInRoute.getDepartureTime()+tv))<=test.getCumulativeWaitingTime()) {
					vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
					if(refRoute.getPartsRoute().get(part2).getListSubJobs().get(0).getId()==1) {
						if(i==1) {
							vehicle.setAmountParamedic(vehicle.getAmountParamedic()+toInsertRoute.getAmountParamedic());
							vehicle.setHomeCareStaff(vehicle.getHomeCareStaff()+toInsertRoute.getHomeCareStaff());
						}
						vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part2));
						part2++;
					}
					vehicle.updateRouteFromParts(inp,test,jobsInWalkingRoute);
					System.out.println("Printing changing 2 " +vehicle.toString());
					changing.removingParts(toInsertRoute.getPartsRoute().get(i));
					changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
					System.out.println("Printing changing 1 " +changing.toString());
					System.out.println("Printing vehicleRouting" +vehicle.toString());
					inserted=true;	
					break;
				}
			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
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
	//			System.out.println("Printing changing 2 " +vehicle.toString());
	//			changing.removingParts(toInsertRoute.getPartsRoute().get(1));
	//			changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
	//			System.out.println("Printing changing 1 " +changing.toString());
	//			System.out.println("Printing vehicleRouting" +vehicle.toString());
	//			inserted=true;	
	//
	//			toInsertRoute.getPartsRoute().clear();
	//			for(Parts part:changing.getPartsRoute()) {
	//				toInsertRoute.getPartsRoute().add(part);
	//			}
	//			toInsertRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
	//			System.out.println("Printing toInsertRoute" +toInsertRoute.toString());}
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
						System.out.println("Printing changing 2 " +vehicle.toString());
						changing.removingParts(toInsertRoute.getPartsRoute().get(i));
						changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
						System.out.println("Printing changing 1 " +changing.toString());
						System.out.println("Printing vehicleRouting" +vehicle.toString());
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
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
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
				System.out.println("Printing changing 2 " +vehicle.toString());
				changing.removingParts(toInsertRoute.getPartsRoute().get(i));
				changing.updateRouteFromParts(inp,test,jobsInWalkingRoute);
				System.out.println("Printing changing 1 " +changing.toString());
				System.out.println("Printing vehicleRouting" +vehicle.toString());
				inserted=true;	

			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test,jobsInWalkingRoute);
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
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
		System.out.println("printing part IR");
		printing(iR.getPartsRoute().get(0));
		printing(iR.getPartsRoute().get(1));
		System.out.println("printing parts JR");
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
		System.out.println("printing part IR");
		System.out.println("part 1");
		printing(iR.getPartsRoute().get(0));
		System.out.println("part 2");
		printing(iR.getPartsRoute().get(1));
		System.out.println("printing parts JR");
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
		// Revisar que el n�mero de personas en el auto no excedan de la capacidad del
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
				System.out.println(r.toString());
				r.updateRouteFromParts(inp,test,jobsInWalkingRoute);
				System.out.println(r.toString());
				r.setIdRoute(routeN);
				initialSol.getRoutes().add(r);
			}
		}
		System.out.println(initialSol);
		// Computar costos asociados a la solucion
		//computeSolutionCost(initialSol);
		System.out.println(initialSol);
		// la lista de trabajos asociados a la ruta

		transferPartsInformation(routeList2);

		// list passengers
		double paramedic=0;
		double homeCoreStaff=0;
		double driver=0;
		for(Route r:routeList2) {
			paramedic+=r.getAmountParamedic();
			homeCoreStaff+=r.getHomeCareStaff();
			driver+=r.getAmountDriver();
		}
		initialSol.setHomeCareStaff(homeCoreStaff);
		initialSol.setParamedic(paramedic);
		return initialSol;
	}




	private void computeStartEndServiceTime(SubJobs jNode) {
		double additionalTime=computeAdditionalTime(jNode);
		double startServiceTime=0;
		double departure=0;
		if(jNode.getTotalPeople()<0) {
			startServiceTime=Math.max((jNode.getArrivalTime()+additionalTime), jNode.getStartTime());
			jNode.setStartServiceTime(startServiceTime);
			departure=startServiceTime+jNode.getReqTime();
			jNode.setEndServiceTime(startServiceTime+jNode.getReqTime());
			jNode.setdepartureTime(departure);
		}
		else {
			startServiceTime=Math.max((jNode.getArrivalTime()), jNode.getStartTime());
			jNode.setStartServiceTime(startServiceTime);
			departure=startServiceTime+jNode.getReqTime()+additionalTime;
			jNode.setEndServiceTime(startServiceTime+jNode.getReqTime());
			jNode.setdepartureTime(departure);
		}

	}

	private boolean changingTimeToTheNextJob(SubJobs lastSubJob, SubJobs firtSubJobToInsert, Route r) {
		// obj: cambiar la hora de empezar el servicio del trabajo que se va a hacer
		// 1. Desde el primer trabajo calcular el timpo de llegada
		boolean merging=false;
		double tv=inp.getCarCost().getCost(lastSubJob.getId()-1, firtSubJobToInsert.getId()-1);
		double possibleArrivalTime= computeArrivalToNexNode(lastSubJob,tv);
		double additiontalTime=computeAdditionalTime(firtSubJobToInsert); // load /unloading time, registratrion time
		if(possibleArrivalTime<=(firtSubJobToInsert.getEndTime()-additiontalTime)) { 	//2. comparar el tiempo de llegada con las ventanas de tiempo
			if(possibleArrivalTime>=firtSubJobToInsert.getStartTime()) {// inicio de la ventana de tiempo
				updatingTimeShift(firtSubJobToInsert,possibleArrivalTime);
				merging=true;
			}
			else { // checking the waiting time  quiere decir que llega m�s temprano que la hora de inicio del servicio
				double additionalTime=computeAdditionalTime(firtSubJobToInsert);
				if((possibleArrivalTime+additionalTime)<=firtSubJobToInsert.getStartTime()) {// se generan tiempos de espera
					double waiting=firtSubJobToInsert.getStartTime()-(possibleArrivalTime+additionalTime);
					if(waiting<=test.getCumulativeWaitingTime()) {
						updatingTimeShift(firtSubJobToInsert,possibleArrivalTime);
						merging=true;
					}
				}
				else {
					merging=true;
				}
			}
		}
		return merging;
	}

	private void updatingTimeShift(SubJobs firtSubJobToInsert, double possibleArrivalTime) {
		// setting arrival time
		firtSubJobToInsert.setarrivalTime(possibleArrivalTime);
		// setting departure time
		computeStartEndServiceTime(firtSubJobToInsert);
		//		double additionalTime=computeAdditionalTime(firtSubJobToInsert);
		//		double serviceStartTime=Math.max((firtSubJobToInsert.getArrivalTime()+additionalTime), firtSubJobToInsert.getStartTime());
		//		firtSubJobToInsert.setStartServiceTime(serviceStartTime);
		//		double departureTime=firtSubJobToInsert.getstartServiceTime()+firtSubJobToInsert.getReqTime();
		//		// 1.1 Set departure time
		//
		//		firtSubJobToInsert.setdepartureTime(departureTime);
		//		// 2.1 setting end service time
		//		firtSubJobToInsert.setEndServiceTime(departureTime);
		System.out.println("start service time"+ firtSubJobToInsert.getstartServiceTime());
		System.out.println("end service time"+ firtSubJobToInsert.getendServiceTime());
	}

	private void insertingHeadingPart(Route r, Schift turn, SubJobs lastSubJob) {
		// 2. inserting the jobs of the first part
		Parts jobsList=turn.getRoute().getPartsRoute().get(0);
		Parts jobsListToInsert=turn.getRoute().getPartsRoute().get(0);
		for(int job=1;job<jobsList.getListSubJobs().size();job++) {
			SubJobs j=jobsList.getListSubJobs().get(job);
			jobsListToInsert.getListSubJobs().add(j);
		}
		r.getPartsRoute().add(jobsListToInsert);
	}
	private void insertingPart(Route r, Schift turn, SubJobs lastSubJob) {
		// 2. inserting the jobs of the first part
		Parts jobsList=turn.getRoute().getPartsRoute().get(0);
		Parts jobsListToInsert=turn.getRoute().getPartsRoute().get(0);
		for(int job=0;job<jobsList.getListSubJobs().size();job++) {
			SubJobs j=jobsList.getListSubJobs().get(job);
			jobsListToInsert.getListSubJobs().add(j);
		}
		r.getPartsRoute().add(jobsListToInsert);

	}

	private boolean checkingTime(Route r, SubJobs lastSubJob, SubJobs firtSubJobToInsert) {
		boolean merging= false;
		boolean timeWindowMatch=false;
		boolean waitingTime=false;
		//1. checking the time - reach job firtSubJobToInsert from lastSubJob
		double tv=inp.getCarCost().getCost(lastSubJob.getId()-1, firtSubJobToInsert.getId()-1);
		double possibleArrivalTime= computeArrivalToNexNode(lastSubJob,tv);
		double neededArribal= computeNeededArrivalToNexNode(firtSubJobToInsert);
		if(possibleArrivalTime<=neededArribal) {	
			double additionalTime=computeAdditionalTime(firtSubJobToInsert);
			if((possibleArrivalTime+additionalTime)<firtSubJobToInsert.getStartTime()) {// se generan tiempos de espera
				double waiting=firtSubJobToInsert.getStartTime()-(possibleArrivalTime+additionalTime);
				if(waiting<=test.getCumulativeWaitingTime()) {
					waitingTime=true;
				}
			}
			else {
				waitingTime=true;
			}
		}
		if(timeWindowMatch && waitingTime) {
			merging= true;
		}
		return merging;
	}

	private boolean checkingDetour(Route r, SubJobs lastSubJob, SubJobs firtSubJobToInsert) {
		boolean detourMax=false;
		//1. checking the time - reach job firtSubJobToInsert from lastSubJob
		double distanceSoFarRoute=computeDistanceRotue(r);
		double maxDetour=computeMaxDetour(firtSubJobToInsert);
		if(maxDetour>=distanceSoFarRoute){
			detourMax=true;
		}
		return detourMax;
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



	private double computeMaxDetour(SubJobs firtSubJobToInsert) {
		double directConnection=inp.getCarCost().getCost(0, firtSubJobToInsert.getId()-1);
		double maxDetour=directConnection*test.getDetour();
		return maxDetour;
	}

	private double computeDistanceRotue(Route r) {
		double distance=0;
		for(Parts part:r.getPartsRoute()) {
			for(int i=1;i<part.getListSubJobs().size();i++) {
				SubJobs iNode=part.getListSubJobs().get(i-1);
				SubJobs jNode=part.getListSubJobs().get(i);
				double tv=inp.getCarCost().getCost(iNode.getId()-1, jNode.getId()-1);
				distance+=tv;
			}
		}
		return distance;
	}

	private double computeNeededArrivalToNexNode(SubJobs firtSubJobToInsert) {
		double neededArribal=0;
		int addionalTime=0;
		if(firtSubJobToInsert.isMedicalCentre()) {// 1. en le caso de que sea un centro m�dico
			addionalTime=test.getRegistrationTime()+test.getloadTimePatient();
		}
		else{
			if(firtSubJobToInsert.isPatient()) {// en el caso en que sea la casa de un paciente
				addionalTime=test.getloadTimePatient();
			}
			else {// es un cliente
				addionalTime=test.getloadTimeHomeCareStaff();
			}
		}
		neededArribal=firtSubJobToInsert.getstartServiceTime()-addionalTime;
		return neededArribal;
	}

	private double computeArrivalToNexNode(SubJobs lastSubJob, double tv) {
		double possibleArrivalTime=lastSubJob.getDepartureTime()+tv;
		return possibleArrivalTime;
	}

	private void sortingEarlyStartShift() {
		//private
		ArrayList<Schift>sortedSchift= new ArrayList<Schift>();
		//HashMap<Integer, Schift> sortedSchift= new HashMap<>();
		// iterando sobre todos los turnos 
		int size=0;
		int t=-1;
		for(Schift turn:splittedSchift) {
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");
			if(sortedSchift.isEmpty()) {
				sortedSchift.add(turn);
			}
			else {
				readingTurns(sortedSchift,turn);
			}
			size=sortedSchift.size()-1;

			/////
			for(Schift copy:sortedSchift) {
				System.out.println("copy turn "+" "+ "Subjob "+copy.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +copy.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");

			}

		}
		splittedSchift.clear();
		t=-1;
		for(Schift turn: sortedSchift) {
			splittedSchift.add(turn);
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(1).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(1).getArrivalTime()+" ");

		}
	}

	private void sortingEarlyEndShift() {
		//private
		ArrayList<Schift>sortedSchift= new ArrayList<Schift>();
		//HashMap<Integer, Schift> sortedSchift= new HashMap<>();
		// iterando sobre todos los turnos 
		int size=0;
		int t=-1;
		for(Schift turn:splittedSchift) {
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");
			if(sortedSchift.isEmpty()) {
				sortedSchift.add(turn);
			}
			else {
				readingTailsTurns(sortedSchift,turn);
			}
			size=sortedSchift.size()-1;
			/////
			for(Schift copy:sortedSchift) {
				System.out.println("copy turn "+" "+ "Subjob "+copy.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +copy.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");
			}
		}
		splittedSchift.clear();
		t=-1;
		for(Schift turn: sortedSchift) {
			splittedSchift.add(turn);
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(1).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(1).getArrivalTime()+" ");
		}
	}

	private void readingTurns(ArrayList<Schift> sortedSchift, Schift turn) {
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es m�s tard�o el el inicio del trabajo de part
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
				// 1. si es m�s temprano que el primer trabajo
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
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es m�s tard�o el el inicio del trabajo de part
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
				// 1. si es m�s temprano que el primer trabajo
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
			System.out.println("Part "+ i);
			for(SubJobs jb:j.getListSubJobs()) {
				System.out.println("subjob "+ jb.getSubJobKey()+ "arival "+ jb.getArrivalTime()+ "Start_service " + jb.getstartServiceTime()+ "  " );
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
		// solo la asignaci�n de trabajos a los niveles de calificaciones // se hace una secuencia ordenad_
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
		List<AttributeNurse> homeCareStaff= inp.getNurse(); // home Care Staff according the qualification level
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
				System.out.println(p.toString());
			}
		}
		sequenceVehicles.clear();
		for(Parts p: partsList) {			
			sequenceVehicles.add(p);
		}


		ArrayList<Route> route=insertingDepotConnections(sequenceVehicles);
		// creaci�n de partes
		Solution newSol= solutionInformation(route); 

		newSol.checkingSolution(inp,test,jobsInWalkingRoute,initialSol);



		System.out.println(newSol.toString());
		Solution mergingRoutes= checkingMergingRoutes(newSol);
		mergingRoutes.checkingSolution(inp, test, jobsInWalkingRoute,initialSol);
		System.out.println("Stop");
		System.out.println(newSol.toString());


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
					System.out.println("merging :" +mergingRoute.toString());		
					if(!r.getSubJobsList().isEmpty()) {
						System.out.println(routeSol.toString());
					}
				}
			}
		}
		routeCopy.clear();

		for(Route r:s.getRoutes() ) {
			if(!r.getSubJobsList().isEmpty()) {
				routeCopy.add(r);
			}
			System.out.println(r.toString());
		}
		s.getRoutes().clear();
		for(Route r:routeCopy) {
			s.getRoutes().add(r);
			System.out.println(r.toString());
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
				System.out.println(routeSol.toString());
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
				System.out.println(j.toString());
			}
			Parts p=disaggregatedJob(j);
			System.out.println(p.toString());
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
				System.out.println(j.toString());
			}
			if(j.getId()==21) {
				System.out.println(j.toString());
			}
			boolean insertesed=false;
			boolean secondPart=false;
			//Couple c1= dropoffHomeCareStaff.get(j.getSubJobKey());
			Couple c= new Couple(dropoffHomeCareStaff.get(j.getSubJobKey()), inp,test);
			SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
			SubJobs pickUp=(SubJobs)c.getStartEndNodes().get(0);
			if(pickUp.getSubJobKey().equals("P38")) {
				System.out.println(j.toString());
			}



			for(Parts paramedic:sequenceVehicles) {
				if(paramedic.getListSubJobs().isEmpty()) {

					insertesed=true;
					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(pickUp);
					pickUpDirectory.remove(pickUp.getSubJobKey());
					System.out.println("Stop");
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
					System.out.println("Stop");	
				}
				Parts newPart=new Parts();
				newPart.getListSubJobs().add(present);
				newPart.getListSubJobs().add(pickUp);
				pickUpDirectory.remove(pickUp.getSubJobKey());
				System.out.println("Stop");
				sequenceVehicles.add(newPart);
				break;
			}
		}


		listSubJobsPickUp.sort(Jobs.SORT_BY_STARTW);

		for(SubJobs present:listSubJobsPickUp) {
			if(present.getSubJobKey().equals("P38")) {
				System.out.println("Stop");
			}
			if(present.getId()==21) {
				System.out.println("Stop");
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
						System.out.println("Stop");
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
						System.out.println("Stop");
					}
					Parts newPart=new Parts();
					newPart.getListSubJobs().add(new SubJobs(present));
					System.out.println("Stop");
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
			System.out.println(p.toString());
			SubJobs dropOffPatient=p.getListSubJobs().get(1);
			listSubJobsDropOff.add(dropOffPatient);
			SubJobs pickUpPatientMC=p.getListSubJobs().get(2);
			listSubJobsPickUp.add(pickUpPatientMC);
		}

		listSubJobsDropOff.sort(Jobs.SORT_BY_STARTW);
		listSubJobsPickUp.sort(Jobs.SORT_BY_STARTW);

		for(SubJobs j:listSubJobsDropOff) {
			if(j.getSubJobKey().equals("D4871")) {
				System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4972")) {
				System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4669")) {
				System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4456")) {
				System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4463")) {
				System.out.println("Stop");
			}
			if(j.getSubJobKey().equals("D4954")) {
				System.out.println("Stop");
			}
			Couple c= new Couple(dropoffpatientMedicalCentre.get(j.getSubJobKey()), inp,test);
			SubJobs present=(SubJobs)c.getStartEndNodes().get(1);
			SubJobs future=(SubJobs)c.getStartEndNodes().get(0);
			for(Parts paramedic:sequenceVehicles) {
				if(paramedic.getDirectoryConnections().containsKey("D4972")) {
					System.out.println("Stop");
				}
				System.out.print("Stop");
				System.out.print(paramedic.toString());
				boolean insertesed=false;
				if(paramedic.getListSubJobs().isEmpty()) {
					insertesed=true;

					paramedic.getListSubJobs().add(present);
					paramedic.getListSubJobs().add(future);
					System.out.println("Stop");
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
				System.out.println("Stop");
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
			System.out.println("stop");
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
			System.out.println("Stop");
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
			System.out.println("Stop");
		}			
		return workingHoras;
	}

	private boolean insertingPairSubJobsPickUpDropOffPatient(SubJobs j, Parts paramedic) {
		boolean merge=false;
		if(j.getSubJobKey().equals("P4759")) {
			System.out.println("Stop");
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
		// solo la asignaci�n de trabajos a los niveles de calificaciones // se hace una secuencia ordenad_
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
		List<AttributeNurse> homeCareStaff= inp.getNurse(); // home Care Staff according the qualification level
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
		System.out.println("all turns");
		int i=-1;
		for(Parts s:schift) {
			i++;
			System.out.println("turn "+i);
			printing(s);
		}

		System.out.println("all turns");

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
		System.out.println("all turns");
	}

	private void downgradings(ArrayList<Parts> highQualification, ArrayList<Parts> lowQualification) {
		// cada parte es un personal
		boolean insertion=false;
		for(Parts high:highQualification) {
			insertion=downgradingsPart(high,lowQualification);

		}	
		System.out.println("Print solution"+insertion );
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
			makeTurnInRoute(turn,route);

		}
		// 2. compute the the start and end time of route
		timeStartEndRoutes(route);
		//3. compute the connections between SubJobs
		settingEdges(route);
		creatingSchifts(route);
		System.out.println("Stop");
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
		for(Route r:route) {
			for(int i=1;i<r.getSubJobsList().size()-1;i++) {
				SubJobs previous=r.getSubJobsList().get(i-1);
				SubJobs job=r.getSubJobsList().get(i);
				Edge e=new Edge(previous,job,inp,test);
				r.getEdges().put(e.getEdgeKey(), e);

			}

			SubJobs depotStart=r.getPartsRoute().get(0).getListSubJobs().get(0);
			Edge e = new Edge(depotStart,r.getSubJobsList().get(0),inp,test);
			r.getEdges().put(e.getEdgeKey(), e);
			SubJobs depotEnd=r.getPartsRoute().get(r.getPartsRoute().size()-1).getListSubJobs().get(0);
			e = new Edge(r.getSubJobsList().get(r.getSubJobsList().size()-1),depotEnd,inp,test);
			r.getEdges().put(e.getEdgeKey(), e);
		}


	}

	private void timeStartEndRoutes(ArrayList<Route> route) {
		// 1. computing times Route
		for(Route r:route) {
			//1. start time
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
		System.out.println(r.toString());
	}

	private void computeStartTimeRoute(SubJobs firstJob, Route r) {
		// 1. Compute travel time
		SubJobs depot=r.getPartsRoute().get(0).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(depot.getId()-1,firstJob.getId()-1);
		double arrivalTime=	firstJob.getArrivalTime()-tv-test.getloadTimeHomeCareStaff();
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
		depot.setserviceTime(0);
		depot.setStartTime(0);
		depot.setEndTime(0);
		System.out.println(r.toString());
	}

	private void makeTurnInRoute(Parts turn, ArrayList<Route> route) { // crea tantas rutas como son posibles
		// la creaci�n de estas rutas lo que hace es identificar las partes de cada turno
		// pero en esencia s�lo deberia agregar el deport

		System.out.println(turn.toString());
		//	calling depot
		//Jobs depot=inp.getNodes().get(0);
		SubJobs depotStart = new SubJobs(inp.getNodes().get(0));
		depotStart.setTotalPeople(1);
		SubJobs depotEnd = new SubJobs(inp.getNodes().get(0));
		depotEnd.setTotalPeople(1);
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
		//					System.out.println(r.toString());
		//				}
		//			}
		//			else {
		//				partObject.getListSubJobs().add(sj);
		//				partObject= new Parts();
		//				r.getPartsRoute().add(partObject);
		//				System.out.println(r.toString());
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
		System.out.println("Route");
		System.out.println(r.toString());
		System.out.println("end");
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
		double tv=inp.getCarCost().getCost(jobsInRoute.getId()-1, dropOff.getId()-1)*test.getDetour();
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
		double tv=inp.getCarCost().getCost(inRoute.getId()-1, pair.getId()-1)*test.getDetour();
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
				if(paramedic.getDirectorySubjobs().containsKey("D19") &&  j.getId()==30) {
					System.out.println("stop");
				}
				if(j.getId()==30) {
					System.out.println(" Turn ");
					System.out.println(j.toString());
				}
				if(j.getId()==19) {
					System.out.println(" Turn ");
					System.out.println(j.toString());
				}
				if(j.getId()==15) {
					System.out.println(" Turn ");
					System.out.println(j.toString());
				}
				if(j.getId()==24) {
					System.out.println(" Turn ");
					System.out.println(j.toString());
				}
				if(j.getId()==25) {
					System.out.println(" Turn ");
					System.out.println(j.toString());
				}

				if(j.getId()==26) {
					System.out.println(" Turn ");
					//System.out.println(j.toString());
				}
				System.out.println(" Turn ");
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
			System.out.println(j.getSubJobKey()+" arrival "+ j.getArrivalTime()+" start time "+ j.getstartServiceTime()+ "req time " + j.getReqTime());
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
				jsplited=pickUpDropOff.getListSubJobs().get(1);
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

			insertingSubJobsIntheSequence(homeCare,dropOffPickUp); // la asignaci�n de la lista de subjobs no necesariamente se hace dentro de un mismo turno
			//j.setarrivalTime(j.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		}
		else { // the job j have to be a medical centre
			Parts pickUpDropOff=splitPatientJobInSubJobs(j);
			insertingSubJobsIntheSequence(homeCare,pickUpDropOff); // la asignaci�n de la lista de subjobs no necesariamente se hace dentro de un mismo turno

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
		double tv=inp.getCarCost().getCost(dropOffPickUp.getListSubJobs().get(0).getId()-1, dropOffPickUp.getListSubJobs().get(1).getId()-1)*test.getDetour();
		double homeDeparture=dropOffPickUp.getListSubJobs().get(1).getArrivalTime()-tv;

		double homeArrival=homeDeparture-dropOffPickUp.getListSubJobs().get(0).getdeltaArrivalDeparture();
		double homeStartServiceTime=homeArrival+dropOffPickUp.getListSubJobs().get(0).getdeltaArrivalStartServiceTime();
		double endServiceTime=homeStartServiceTime+dropOffPickUp.getListSubJobs().get(0).getdeltarStartServiceTimeEndServiceTime();
		dropOffPickUp.getListSubJobs().get(0).setarrivalTime(homeArrival);
		dropOffPickUp.getListSubJobs().get(0).setStartServiceTime(homeStartServiceTime);
		dropOffPickUp.getListSubJobs().get(0).setEndServiceTime(endServiceTime);
		endServiceTime=dropOffPickUp.getListSubJobs().get(1).getendServiceTime();
		for(int i=2; i<dropOffPickUp.getListSubJobs().size();i++ ) {
			SubJobs sbi=dropOffPickUp.getListSubJobs().get(i-1);
			SubJobs sbj=dropOffPickUp.getListSubJobs().get(i);
			tv= inp.getCarCost().getCost(sbi.getId()-1, sbj.getId()-1)*test.getDetour();
			double arrivalTime=endServiceTime+tv;

			double startServiceTime=arrivalTime+sbj.getdeltaArrivalStartServiceTime();
			double departureTime=arrivalTime+sbj.getdeltaArrivalDeparture();
			endServiceTime=startServiceTime+sbj.getdeltarStartServiceTimeEndServiceTime();
			sbj.setarrivalTime(arrivalTime);
			sbj.setStartServiceTime(startServiceTime);
			sbj.setEndServiceTime(endServiceTime);
			sbj.setdepartureTime(departureTime);	
			// setting new tw
			sbj.setStartTime(startServiceTime);
			sbj.setEndTime(startServiceTime);
		}
		System.out.println(dropOffPickUp.toString());	
	}


	private void settingEarlyTimeFutureJob(Parts dropOffPickUp) {

		double endServiceTime=dropOffPickUp.getListSubJobs().get(0).getendServiceTime();
		for(int i=1; i<dropOffPickUp.getListSubJobs().size();i++ ) {
			SubJobs sbi=dropOffPickUp.getListSubJobs().get(i-1);
			SubJobs sbj=dropOffPickUp.getListSubJobs().get(i);
			double tv= inp.getCarCost().getCost(sbi.getId()-1, sbj.getId()-1)*test.getDetour();
			double arrivalTime=endServiceTime+tv;

			double startServiceTime=arrivalTime+sbj.getdeltaArrivalStartServiceTime();
			double departureTime=arrivalTime+sbj.getdeltaArrivalDeparture();
			endServiceTime=startServiceTime+sbj.getdeltarStartServiceTimeEndServiceTime();
			sbj.setarrivalTime(arrivalTime);
			sbj.setStartServiceTime(startServiceTime);
			sbj.setEndServiceTime(endServiceTime);
			sbj.setdepartureTime(departureTime);
			// definici�n de las nuevas ventanas de tiempo
			sbj.setStartTime(startServiceTime);
			sbj.setEndTime(startServiceTime);
		}
		System.out.println(dropOffPickUp.toString());	
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
		Parts newPart= new Parts();
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();// considerar el inicio y el fin del servicio
		// 2. Generation del drop off at medical centre
		//		Jobs j1patient=	coupleList.get(j.getSubJobKey()).getPresent();
		//		Jobs j1dropOffMedicalCentre=	coupleList.get(j.getSubJobKey()).getFuture();
		Couple c=coupleList.get(j.getSubJobKey());
		Jobs patient= new Jobs(c.getPresent());
		SubJobs dropOffMedicalCentre= new SubJobs(c.getFuture());

		//settingTimeDropOffPatientParamedicSubJob(dropOffMedicalCentre,j);


		// 1. Generation del pick up at patient home 
		SubJobs pickUpPatientHome= new SubJobs(c.getPresent());
		//	settingTimePickUpPatientSubJob(pickUpPatientHome,dropOffMedicalCentre);
		//Couple n=new Couple(pickUpPatientHome, dropOffMedicalCentre);
		//		coupleList.put(pickUpPatientHome.getSubJobKey(), n);
		//		coupleList.put(dropOffMedicalCentre.getSubJobKey(), n);
		dropoffpatientMedicalCentre.put(dropOffMedicalCentre.getSubJobKey(), c);
		//dropOffMedicalCentre.setPair(pickUpPatientHome);
		// 3. Generaci�n del pick at medical centre
		String key="D"+patient.getId();
		Couple c1=coupleList.get(key);
		SubJobs pickUpMedicalCentre= new SubJobs(c1.getPresent());
		//settingTimePickUpPatientParamedicSubJob(pickUpMedicalCentre,dropOffMedicalCentre);

		// 4. Generaci�n del drop-off at client home
		SubJobs dropOffPatientHome= new SubJobs(c1.getFuture());
		//	settingTimeDropOffPatientSubJob(dropOffPatientHome,pickUpMedicalCentre);
		//n=new Couple(pickUpMedicalCentre, dropOffPatientHome);
		//		coupleList.put(pickUpMedicalCentre.getSubJobKey(), n);
		//		coupleList.put(dropOffPatientHome.getSubJobKey(), n);
		pickpatientMedicalCentre.put(pickUpMedicalCentre.getSubJobKey(), c1);
		//dropOffPatientHome.setPair(pickUpMedicalCentre);
		// 3. Addding the subjobs to the list
		subJobsList.add(pickUpPatientHome); // Se apilan por orden de sequencia
		subJobsList.add(dropOffMedicalCentre);
		subJobsList.add(pickUpMedicalCentre); // Se apilan por orden de sequencia
		subJobsList.add(dropOffPatientHome);
		newPart.setListSubJobs(subJobsList, inp, test);
		return newPart;
	}

	private void settingTimeDropOffPatientSubJob(SubJobs dropOffPatientHome, Jobs pickUpMedicalCentre) {
		//()--------------(j)-------------(dropOffPatientHome)
		dropOffPatientHome.setTotalPeople(-1);
		dropOffPatientHome.setPatient(true);
		dropOffPatientHome.setloadUnloadTime(test.getloadTimePatient());
		// 1. Setting the start service time -- startServiceTime
		double travel=inp.getCarCost().getCost(pickUpMedicalCentre.getId()-1, dropOffPatientHome.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones
		double arrivalTime=pickUpMedicalCentre.getDepartureTime()+travel;  // load al lugar de la cita medica
		dropOffPatientHome.setarrivalTime(arrivalTime);
		dropOffPatientHome.setStartTime(arrivalTime);
		dropOffPatientHome.setEndTime(arrivalTime);// departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al veh�culo
		// modificar el tiempo requerido para el trabajo+
		dropOffPatientHome.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		dropOffPatientHome.setStartServiceTime(dropOffPatientHome.getEndTime());
		// 3. Set el fin del servicio
		dropOffPatientHome.setEndServiceTime(dropOffPatientHome.getstartServiceTime()+test.getloadTimePatient());
		dropOffPatientHome.setdepartureTime(dropOffPatientHome.getendServiceTime());
	}

	private void settingTimePickUpPatientParamedicSubJob(SubJobs pickUpMedicalCentre, Jobs j) {
		//()--------------(pickUpMedicalCentre=j)-------------()-------------()
		pickUpMedicalCentre.setTotalPeople(2); // 5. Setting the total people (+) pick up   (-) drop-off
		pickUpMedicalCentre.setMedicalCentre(true);
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
		// 1. Generaci�n del drop-off job

		Couple c=coupleList.get(j.getSubJobKey());
		SubJobs dropOff= new SubJobs(c.getPresent());
		// 2. Generaci�n del pick-up job
		SubJobs pickUp= new SubJobs(c.getFuture());
		//settingTimeClientSubJob(dropOff,pickUp);
		// 3. Addding the subjobs to the list
		subJobsList.add(dropOff); // Se apilan por orden de sequencia
		subJobsList.add(pickUp);
		//Couple n=new Couple(dropOff,pickUp);
		//	coupleList.put(dropOff.getSubJobKey(), n);
		//coupleList.put(pickUp.getSubJobKey(), n);
		dropoffHomeCareStaff.put(dropOff.getSubJobKey(), c);
		pickUpHomeCareStaff.put(pickUp.getSubJobKey(), c);
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
		pickUp.setserviceTime(dropOff.getReqTime()); //los nodos pick up contienen la informaci�n de los nodos
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
			System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());	
		}
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteraci�n sobre el turno y se inserta
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
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());

		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteraci�n sobre el turno y se inserta
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
			// se intenta insertar antes - El trabajo importante es j porque k es la continuaci�n
			double tv=inp.getCarCost().getCost(k.getId()-1, firstInRoute.getId()-1)*test.getDetour();
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
		double travelTime=inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1)*test.getDetour();
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
				System.out.println("Stop");
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
						c.getFuture().setPatient(true);}
					if(inp.getMedicalCentre().containsKey(c.getFuture().getId())) {
						c.getPresent().setMedicalCentre(true);}
					subJobspatients.add(c);



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
