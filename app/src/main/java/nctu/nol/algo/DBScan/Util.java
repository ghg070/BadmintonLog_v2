package nctu.nol.algo.DBScan;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import android.graphics.Point;


public class Util {
	public Vector<CustomPoint> VisitList;

	public Util(){
		VisitList = new Vector<CustomPoint>();
	}
	
	public static Vector<CustomPoint> getNeighbours(ArrayList<CustomPoint> draw_list, CustomPoint p, double eps)
	{
		Vector<CustomPoint> neigh =new Vector<CustomPoint>();
		Iterator<CustomPoint> points = draw_list.iterator();
		while(points.hasNext()){
			CustomPoint q = points.next();
			if(true == judgeInCircle(p, q, eps)){
				neigh.add(q);
			}
		}
		return neigh;
	}

	public void Visited(CustomPoint d){
		VisitList.add(d);
	}

	public boolean isVisited(CustomPoint c)
	{
		if (VisitList.contains(c))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public static boolean judgeInCircle(CustomPoint p, CustomPoint p2, double eps){
		
		double dist;
	    boolean result = false;
		
	    dist = Math.sqrt(Math.pow(p.x - p2.x, 2) + Math.pow(p.y - p2.y, 2));
		if(dist <= eps){
			result = true;
		}
		return result;
	}
	
	public Vector<CustomPoint> Merge(Vector<CustomPoint> a, Vector<CustomPoint> b)
	{
		Iterator<CustomPoint> it = b.iterator();
		while(it.hasNext()){
			CustomPoint t = it.next();
			if (!a.contains(t) ){
				a.add(t);
			}
		}	
		return a;
	}
	
	public static class CustomPoint extends Point{
		public boolean noized = false;
		public CustomPoint(int x, int y){
			super(x, y);
		}
		
		public void setNoized(){
			noized = true;
		}
	}
	
	public static class Cluster {
		int cluster_id;	
		ArrayList<CustomPoint> CustomPointList; 
		
		public Cluster(){
			this.CustomPointList = new ArrayList<CustomPoint>();
		}
		public Cluster(ArrayList<CustomPoint> plist){
			this.CustomPointList = plist;
		}
		public void addCustomPoint(CustomPoint newone){
			this.CustomPointList.add(newone);
		}
		
		public ArrayList<CustomPoint> addCustomPointList(Vector<CustomPoint> b){
			Iterator<CustomPoint> itr = b.iterator();
			while(itr.hasNext()){
				CustomPoint t = itr.next();
				if (!this.CustomPointList.contains(t) ){
					this.CustomPointList.add(t);
				}
			}	
			return this.CustomPointList;
		}
		
		public void dellCustomPoint(CustomPoint dellone){
			this.CustomPointList.remove(dellone);
		}
		public void alldellCustomPoint(){
			this.CustomPointList.clear();
		}
		public void setClusterID(int clst_id){
			this.cluster_id = clst_id;
		}
		public int getClusterID(){
			return this.cluster_id;
		}
		public ArrayList<CustomPoint> getCustomPointList(){
			return CustomPointList;
		}

	}
}