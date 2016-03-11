package nctu.nol.algo.DBScan;

import java.util.ArrayList;
import java.util.Vector;

import nctu.nol.algo.DBScan.Util.Cluster;
import nctu.nol.algo.DBScan.Util.CustomPoint;



public class DBScan {
	ArrayList<Cluster> clstr_list = new ArrayList<Cluster>();
	Vector<CustomPoint> neighber_list;
	Vector<CustomPoint> neighber_list2;
	Util util;
	
	public DBScan(){}
	
	public void doClustering(ArrayList<CustomPoint> allpoint, int minPts, int eps ){
	
	    this.util = new Util();
		
		for(CustomPoint obj:allpoint){
			if(util.isVisited(obj) == false){
				util.Visited(obj);
				neighber_list = Util.getNeighbours(allpoint, obj, eps);
				if(neighber_list.size() < minPts){
					obj.setNoized();
				}else{
					Vector<CustomPoint>neighbours = expandCluster(neighber_list, allpoint, eps, minPts);
					Cluster tmpclstr = new Cluster();
					tmpclstr.addCustomPointList(neighbours);
					clstr_list.add(tmpclstr);
				}
			}
		}
	}
	
	private Vector<CustomPoint> expandCluster(Vector<CustomPoint>neighber_list, ArrayList<CustomPoint>draw_list, int eps, int minPts){
		
		int index = 0;
		while(neighber_list.size() > index){
			CustomPoint tmppoint = neighber_list.get(index);
			if(this.util.isVisited(tmppoint) == false){
				this.util.Visited(tmppoint);
				neighber_list2 = Util.getNeighbours(draw_list, tmppoint, eps);
				if(neighber_list2.size() >= minPts){
					this.util.Merge(neighber_list, neighber_list2);
				}
			}			
			index++;
		}
		return neighber_list;
	}
	
}