import java.util.ArrayList;
import java.util.HashMap;

public class Parts {
	private String key="";
	private int qualificationLevel=-1;
	private boolean paramedicSchift=false;
	private boolean homecareStaffSchift=false;
	private boolean driver=false;
	private  Route routeVehicle; // the shift is a route in order to count the working hours
	private  ArrayList<SubJobs> listSubJobs= new ArrayList<>(); // the shift is a route in order to count the working hours
	private  SubJobs headPart;
	private SubJobs nodeReference;
	private  HashMap<String, SubJobs> directorySubjobs= new HashMap<>(); // the shift is a route in order to count the working hours
	private  HashMap<String, Edge> directoryConnections= new HashMap<>(); // las conecciones entre las partes// este me permite controlar los subtours


	// set of getters

	public Parts(Parts part) {
		key=part.getKey();
		qualificationLevel=part.getQualificationLevel();
		paramedicSchift=part.isParamedicSchift();
		homecareStaffSchift=part.isHomecareStaffSchift();
		driver=part.isDriver();
		for(SubJobs n:part.getListSubJobs()) {
			Jobs formalJob=new Jobs(n);
			listSubJobs.add(new SubJobs(formalJob));
			directorySubjobs.put(n.getSubJobKey(), n);
		}
		for(Edge e:part.getDirectoryConnections().values()) {
			Edge eNew= new Edge(e);
			this.directoryConnections.put(eNew.getEdgeKey(), eNew);
		}
	}


	public Parts() {
		listSubJobs= new ArrayList<>(); // the shift is a route in order to count the working hours
		directorySubjobs= new HashMap<>(); // the shift is a route in order to count the working hours
		directoryConnections= new HashMap<>(); // las conecciones entre las partes// este me permite controlar los subtours
	}



	// Getters

	public String getKey() {return key;}
	public int getQualificationLevel() {return qualificationLevel;}
	public boolean isParamedicSchift() {return paramedicSchift;}
	public boolean isHomecareStaffSchift() {return homecareStaffSchift;}
	public boolean isDriver() {	return driver;}
	public Route getRouteVehicle() {return routeVehicle;}
	public ArrayList<SubJobs> getListSubJobs() {return listSubJobs;}
	public SubJobs getmainElement() {return headPart;}
	public HashMap<String, SubJobs> getDirectorySubjobs() {return directorySubjobs;}
	public HashMap<String, Edge> getDirectoryConnections() {return directoryConnections;}
	public SubJobs getReferenceNode() {return nodeReference;}

	// Setters
	public void setKeyParts(String k) {this.key = k;}

	public void setListSubJobs(ArrayList<SubJobs> listSubJobs, Inputs inp, Test test) {
		this.listSubJobs = listSubJobs;
		for(SubJobs j:listSubJobs) {
			if(j.getReqQualification()>qualificationLevel) {
				qualificationLevel=j.getReqQualification();
			}
		}

		settingConnections(listSubJobs,inp,test);

		// Setting the reference node
		for(SubJobs j:listSubJobs) {
			if(j.isMedicalCentre() && j.getTotalPeople()<0) {
				nodeReference=	j;
			}
			else {
				if(j.isClient() && j.getTotalPeople()<0) {
					nodeReference=	j;
				}
			}
		}

	}


	public void settingConnections(ArrayList<SubJobs> listSubJobs2, Inputs inp, Test test) {
		// setting the connections
		directoryConnections.clear();
		for(int i=0;i<listSubJobs.size()-1;i++) {
			SubJobs iNode=listSubJobs.get(i);
			SubJobs jNode=listSubJobs.get(i+1);
			Edge e= new Edge(iNode,jNode, inp,test);
			directoryConnections.put(e.getEdgeKey(), e);
		}
		for(Edge e:directoryConnections.values()) {
			System.out.print(e.toString());
		}
		System.out.println("end");

	}

	public String toString() 
	{   String s = "";
	s = s.concat("\nID part: " + key);
	s = s.concat("\n List of jobs: ");
	s= s.concat("\n");
	for(SubJobs j:this.getListSubJobs()) {	
		s = s.concat(" ( " + j.getSubJobKey()+", A_"+j.getArrivalTime()+", B_"+j.getstartServiceTime()+" end serv "+ j.getendServiceTime()+" D_"+j.getDepartureTime()+" TW ["+j.getStartTime()+","+j.getEndTime()+"] "+", reqTime_"+j.getReqTime()+") \n");
	}
	return s;
	}


	public void settingConnections(Parts parts, Parts parts2) {
		this.getDirectoryConnections().clear();
		for(Edge e:parts.getDirectoryConnections().values()) {
		this.getDirectoryConnections().put(e.getEdgeKey(),e);	
		}
		for(Edge e:parts2.getDirectoryConnections().values()) {
			this.getDirectoryConnections().put(e.getEdgeKey(),e);	
			}
		
	}


}
