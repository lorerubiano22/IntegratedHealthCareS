
public class Edge implements Comparable<Edge>
{
    /* INSTANCE FIELDS & CONSTRUCTOR */
    private Jobs origin; // origin node
    private Jobs end; // end node
    private double time = 0.0; // edge travel time
    private Edge inverseEdge = null; // edge with inverse direction
            
    public Edge(Jobs originNode, Jobs endNode) 
    {   origin = originNode;
        end = endNode;
    }

    /* SET METHODS */
    public void setTime(double c){time = c;}
    public void setInverse(Edge e){inverseEdge = e;}

    /* GET METHODS */
    public Jobs getOrigin(){return origin;}
    public Jobs getEnd(){return end;}
    public double getTime(){return time;}
    public Edge getInverseEdge(){return inverseEdge;}


    
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
