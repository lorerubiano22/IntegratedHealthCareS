import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Jobs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	private final int id; // node ID (depotID = 0)
	private final int startTime; // node x coordinate ||final removed for preprocessing test
	private final int endTime; // node y coordinate || final removed for preprocessing test
	private final int reqQualification;
	private final int startServiceTime=0; // time when the service start
	private int serviceTime; //final removed for preprocessing test
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
		this.startTime = startTime;
		this.endTime = endTime;
		this.reqQualification = reqQualification;
		this.serviceTime = reqTime;
		this.jobSet = formedBy;
	}

	public Jobs getDupe()
	{
		Jobs dupe = new Jobs(id,startTime,endTime,reqQualification,serviceTime,jobSet);
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
		return startTime;
	}
	
	public int getEndTime() {
		return endTime;
	}

	public int getReqQualification() {
		return reqQualification;
	}

	public int getReqTime() {
		return serviceTime;
	}

	public String extendedData() {
		return String.format("(id = %d , RQ = %d\n ST: %d,  RT: %d, ET: %d + [PB: %d])",id,reqQualification,startTime,serviceTime,endTime,endTime - serviceTime);
	}

	public static Comparator<Jobs> SORT_BY_ENDTW = new Comparator<Jobs>() {

		@Override
		public int compare(Jobs o1, Jobs o2) {
			return (int) (o1.getEndTime() - o2.getEndTime());
		}

	};



	public int getTW() { return endTime-startTime; }



}
