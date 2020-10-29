
public class Edge implements Comparable<Edge>
{
	/* INSTANCE FIELDS & CONSTRUCTOR */
	private String key="";
	private SubJobs origin; // origin node
	private SubJobs end; // end node
	private double time = 0.0; // edge travel time
	private double detour = 0.0; // edge travel time
	private Edge inverseEdge = null; // edge with inverse direction

	public Edge(SubJobs originNode, SubJobs endNode, Inputs inp, Test test) 
	{
		origin = originNode;
		end = endNode;
		time=inp.getCarCost().getCost(originNode.getId()-1, endNode.getId()-1);
		key=originNode.getSubJobKey()+endNode.getSubJobKey();
		detour=inp.getCarCost().getCost(originNode.getId()-1, endNode.getId()-1)*test.getDetour();
	}

	public Edge(Edge e) {
		origin = e.getOrigin();
		end = e.getEnd();
		time=e.getTime();
		key=e.getEdgeKey();
		detour=e.getDetour();
	}

	/* SET METHODS */
	public void setTime(double c){time = c;}
	public void setInverse(Edge e){inverseEdge = e;}
	public void setIdKey(String k) { key=k;}

	/* GET METHODS */
	public SubJobs getOrigin(){return origin;}
	public SubJobs getEnd(){return end;}
	public double getTime(){return time;}
	public double getDetour(){return detour;}
	public Edge getInverseEdge(){return inverseEdge;}
	public String getEdgeKey(){return key;}



	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nEdge origin job: " + this.getOrigin());
	s = s.concat("\nEdge end job: " + this.getEnd());
	s = s.concat("\nEdge time: " + (this.getTime()));
	return s;
	}

	@Override
	public int compareTo(Edge arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
}
