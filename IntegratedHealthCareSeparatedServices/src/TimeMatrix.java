import java.util.List;



public class TimeMatrix {
	private final int[][] timeMatrix;

	private TimeMatrix(final int[][] timeMatrix) {
		this.timeMatrix = timeMatrix;
	}

	public final int getCost(final int src,final int dst) {
		int costs = timeMatrix[src][dst];
		return costs;
	}
	
	public static TimeMatrix getCostMatrix(int[][] costMatrix) {
		return new TimeMatrix(costMatrix);
	}

	public static int[][] fromList(List<Integer> cList) {
		int sz = (int) Math.sqrt(cList.size());
		int[][] matrix = new int[sz][sz];
		int i = 0,j = 0;
		for(int n: cList) {
			matrix[i][j++] = n;
			if(j == sz) {
				i++;
				j = 0;
			}
		}
		return matrix;
	}
	
	public void mergeWR(Jobs c1, Jobs c2) 
	{
		this.timeMatrix[c1.getId()] = this.timeMatrix[c2.getId()];
		this.timeMatrix[c1.getId()][c1.getId()] = 0;
	}
	
}
