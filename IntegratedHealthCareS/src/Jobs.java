import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Jobs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	private final int id; // node ID (depotID = 0)
	private final int hardstartTime; // input data early time window 
	private final int hardendTime; // input data end time window
	private final int softstartTime; // estimated early soft time window
	private final int softendTime; // estimated end soft time window 
	private final int reqQualification;
	private final int startServiceTime=0; // time when the service start
	private int serviceTime; // required service time
	private int sortETWSizeCriterion; // sort criterion the size of time window and the earliest time
	private int sortLTWSizeCriterion; // sort criterion the size of time window and the latest time
	private final List<Jobs> jobSet;
	public final static Jobs DEPOT_NODE = new Jobs(0, 0, 0, 0, 0);

	private boolean isServerd = false;
	

	/* SET METHODS */

	public boolean isServerd() {
		return isServerd;
	}

	public void setServerd(boolean isServerd) {
		this.isServerd = isServerd;
	}
	public void setserviceTime(int B) {
		this.serviceTime = B;
	}

	public Jobs(int id, int startTime, int endTime, int reqQualification,
			int reqTime) {
		this(id,startTime,endTime,reqQualification,reqTime,new LinkedList<Jobs>());
	}

	public Jobs(int id, int startTime, int endTime, int reqQualification,
			int reqTime, List<Jobs> formedBy) {
		this.id = id;
		this.hardstartTime = startTime;
		this.hardendTime = endTime;
		this.softstartTime = startTime;
		this.softendTime = endTime;
		this.reqQualification = reqQualification;
		this.serviceTime = reqTime;
		this.jobSet = formedBy;
		int sizeTW=hardendTime-hardstartTime;
		this.sortETWSizeCriterion=(startTime)*(sizeTW);
		this.sortLTWSizeCriterion=(endTime)*(sizeTW);
	}

	public Jobs getDupe()
	{
		Jobs dupe = new Jobs(id,hardstartTime,hardendTime,reqQualification,serviceTime,jobSet);
		dupe.isServerd = isServerd;
		return dupe;
	}

	/* GET METHODS */

	public int getId() {
		return id;
	}

	public int getstartServiceTime() {
		return startServiceTime;
	}

	public int getStartTime() {
		return hardstartTime;
	}
	
	public int getEndTime() {
		return hardendTime;
	}
	
	public int getSoftStartTime() {
		return this.softstartTime;
	}
	
	public int getSoftEndTime() {
		return this.softendTime;
	}
	public int getsortETWSizeCriterion() {
		return this.sortETWSizeCriterion;
	}
	
	public int getsortLTWSizeCriterion() {
		return this.sortLTWSizeCriterion;
	}
	
	public int getReqQualification() {
		return reqQualification;
	}

	public int getReqTime() {
		return serviceTime;
	}

	public String extendedData() {
		return String.format("(id = %d , RQ = %d\n ST: %d,  RT: %d, ET: %d + [PB: %d])",id,reqQualification,hardstartTime,serviceTime,hardendTime,hardendTime - serviceTime);
	}

	public static Comparator<Jobs> SORT_BY_ENDTW = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getEndTime() < o2.getEndTime())
				return 1;
			if (o1.getEndTime() > o2.getEndTime())
				return -1;
			return 0;
		}

	};

	
	public static Comparator<Jobs> SORT_BY_STARTW = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getStartTime() < o2.getStartTime())
				return 1;
			if (o1.getStartTime() > o2.getStartTime())
				return -1;
			return 0;
		}
	};
	
	public static Comparator<Jobs> TWSIZE_Early = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getsortETWSizeCriterion() < o2.getsortETWSizeCriterion())
				return 1;
			if (o1.getsortETWSizeCriterion() > o2.getsortETWSizeCriterion())
				return -1;
			return 0;
		}

	};
	
	public static Comparator<Jobs> TWSIZE_Latest = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getsortLTWSizeCriterion() < o2.getsortLTWSizeCriterion())
				return 1;
			if (o1.getsortLTWSizeCriterion() > o2.getsortLTWSizeCriterion())
				return -1;
			return 0;
		}

	};




	public int getTW() { return hardendTime-hardstartTime; }
	@Override
	public String toString() {
		String s = "";
		s = s.concat("\nJob Id: " + this.id);
		s = s.concat("\nhardstartTime: " + this.hardstartTime);
		s = s.concat("\nhardendTime: " + (this.hardendTime));
		s = s.concat("\nsoftstartTime: " + (this.softstartTime));
		s = s.concat("\nsoftendTime: " + (this.softendTime));
		s = s.concat("\nreqQualification: " + (this.reqQualification));
		s = s.concat("\nstartServiceTime: " + (this.startServiceTime));
		s = s.concat("\nserviceTime: " + (this.serviceTime));
		return s;
	}


}
