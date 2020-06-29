import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class Jobs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	private final int id; // node ID (depotID = 0)
	private final int startTime; // node x coordinate ||final removed for preprocessing test
	private final int endTime; // node y coordinate || final removed for preprocessing test
	private final int reqQualification;
	private final int reqTime; //final removed for preprocessing test
	private final List<Jobs> formedBy;
	public final static Jobs DEPOT_NODE = new Jobs(0, 0, 0, 0, 0);

	private boolean isServerd = false;
	private int eventTime; // time at which a new nurse is activated

	/* SET METHODS */

	public boolean isServerd() {
		return isServerd;
	}

	public void setServerd(boolean isServerd) {
		this.isServerd = isServerd;
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
		this.reqTime = reqTime;
		this.formedBy = formedBy;
		this.eventTime = startTime;
	}

	public Jobs getDupe()
	{
		Jobs dupe = new Jobs(id,startTime,endTime,reqQualification,reqTime,formedBy);
		dupe.isServerd = isServerd;
		return dupe;
	}

	/* GET METHODS */

	public int getId() {
		return id;
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
		return reqTime;
	}

	public String extendedData() {
		return String.format("(id = %d , RQ = %d\n ST: %d,  RT: %d, ET: %d + [PB: %d])",id,reqQualification,startTime,reqTime,endTime,endTime - reqTime);
	}

	@Override
	public String toString() {
		return String.format("(id=%d, RQ=%d, ST=%d, ET=%d, RT=%d)", id,reqQualification,startTime, endTime, reqTime);
		//		return "Node [id=" + id + ", startTime=" + startTime + ", endTime="
		//				+ endTime + ", reqQualification=" + reqQualification
		//				+ ", reqTime=" + reqTime + "]"
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((formedBy == null) ? 0 : formedBy.hashCode());
		result = prime * result + id;
		return result;
	}

	public static Comparator<Jobs> SORT_BY_ENDTW = new Comparator<Jobs>() {

		@Override
		public int compare(Jobs o1, Jobs o2) {
			return (int) (o1.getEndTime() - o2.getEndTime());
		}

	};

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Jobs other = (Jobs) obj;
		if (formedBy == null) {
			if (other.formedBy != null)
				return false;
		} else if (!formedBy.equals(other.formedBy))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

	public int getTW() { return endTime-startTime; }

	/**
	 * @return the eventTime
	 */
	public int getEventTime() {
		return eventTime;
	}

	/**
	 * @param eventTime the eventTime to set
	 */
	public void setEventTime(int eventTime) {
		this.eventTime = eventTime;
	}

}
