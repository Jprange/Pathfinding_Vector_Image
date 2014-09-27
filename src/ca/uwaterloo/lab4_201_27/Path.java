package ca.uwaterloo.lab4_201_27;

import java.util.ArrayList;
import java.util.List;

import android.graphics.PointF;
import android.util.Log;
import android.widget.TextView;

public class Path
{
	
	NavigationalMap	map	= new NavigationalMap();
	TextView		out;
	MapView			mv;
	
	//Constructor
	public Path(NavigationalMap map, TextView output, MapView mv)
	{
		this.map = map;
		out = output;
		this.mv = mv;
	}
	
	private ArrayList<PointF> points	= new ArrayList<PointF>();
	
	// returns the created path
	public List<PointF> getPoints()
	{
		
		return points;
	}
	
	// adds points to the path
	void addPoint(PointF point)
	{
		points.add(point);
	}
	
	// call to create path
	public void createPath(PointF start, PointF end)
	{
		points.clear();
		createPaths(start, end);
	}
	
	public void createPaths(PointF start, PointF end)
	{
		
		//Add origin to path
		points.add(start);
		PointF prevPoint = start;
		List<InterceptPoint> intersections = map.calculateIntersections(start, end);
		int count = 0;
		//Arbitrary 40 attempts at creating a path (will rarely go above 5) 
		
		List<LineSegment> geo = map.getGeometry();
		float maxY = 0;
		
		for (LineSegment seg : geo)
		{
			if (seg.start.y > maxY)
			{
				maxY = seg.start.y;
			}
			if (seg.end.y > maxY)
			{
				maxY = seg.end.y;
			}
		}
		
		
		while (!intersections.isEmpty() && count < 41)
		{
			
			InterceptPoint firstInter = intersections.get(0);
			PointF interPoint = firstInter.getPoint();
			
			Log.d("PATHFINDER", "Intersection Point: " + interPoint.toString());
			
			List<LineSegment> mapSegments = new ArrayList<LineSegment>();
			
			String orientation = "";
			
			//Goes through list of all line segments on map 
			for (LineSegment seg : geo)
			{
				//Checks if the line segment is more or less vertical 
				if (Math.abs(seg.start.x - seg.end.x) < 0.3)
				{
					//more strict check for if the point has same x as segment 
					if (Math.abs(seg.start.x - interPoint.x) < 0.1)
					{
						//check if the y is the point lies on the segment 
						if (!((seg.start.y < interPoint.y && seg.end.y < interPoint.y) || (seg.start.y > interPoint.y && seg.end.y > interPoint.y)))
						{
							Log.d("PATHFINDER", "VERT Chose Seg Y vals: " + seg.start.y + " , " + seg.end.y);
							//add segment to list of possible line segments, identify line segment as vertical 
							mapSegments.add(new LineSegment(seg.start, seg.end));
							orientation = "vert";
						}
					}
				}
				else
				{
					//check if point and segment have same y value
					if (Math.abs(seg.start.y - interPoint.y) < 0.1)
					{
						//check if x of point lies on the segment 
						if (!((seg.start.x < interPoint.x && seg.end.x < interPoint.x) || (seg.start.x > interPoint.x && seg.end.x > interPoint.x)))
						{
							Log.d("PATHFINDER", "HORIZ Chose Seg X vals: " + seg.start.x + " , " + seg.end.x);
							Log.d("PATHFINDER", "Intersection Point: " + interPoint.toString());
							//add segment to possible segments, identify segment as horizontal 
							mapSegments.add(new LineSegment(seg.start, seg.end));
							orientation = "horiz";
						}
					}
				}
				
			}
			//the current intersection being solved is last object in the array 
			LineSegment wall = mapSegments.get(mapSegments.size() - 1);
			
			//identify start and end of the chosen segment 
			PointF startSeg = wall.start;
			PointF endSeg = wall.end;
			
			Log.d("PATHFINDER", "YSeg1: " + startSeg.y + " Seg2: " + endSeg.y);
			Log.d("PATHFINDER", "XSeg1: " + startSeg.x + " Seg2: " + endSeg.x);
			
			Log.d("PATHFINDER", orientation);
			
		
			float finX;
			float finY;
			PointF usedPoint = null;
			//check orientation of the chosen segment 
			if (orientation.equals("vert"))
			{
				Log.d("PATHFINDER", "VERT CHOSEN");
				//choose the lowest point, go down extra 1(no path through the top)  
				if (startSeg.y < endSeg.y)
				{
					finY = endSeg.y + (float) 1;
					finX = endSeg.x;
					if (finY > maxY)
					{
						finY = startSeg.y - 1;
					}
				}
				else
				{
					finX = startSeg.x;
					finY = startSeg.y + (float) 1;
					if (finY > maxY)
					{
						finY = endSeg.y - 1;
					}
				}
			}
			else
			{
				Log.d("PATHFINDER", "Horiz CHOSEN");
				
				
				//Segment is horizontal 
				
				//check if intersection is left or right of destination, shift final point towards destination
				if ((end.x < startSeg.x))
				{
					if (startSeg.x < endSeg.x)
					{
						usedPoint = startSeg;
					}
					else
					{
						usedPoint = endSeg;
					}
					finX = usedPoint.x - (float) 1;
					finY = usedPoint.y;
				}
				else 
				{
					if (startSeg.x > endSeg.x)
					{
						usedPoint = startSeg;
					}
					else
					{
						usedPoint = endSeg;
					}
					
					finX = usedPoint.x + (float) 1;
					finY = usedPoint.y;
				}
				
				
			}
			
			PointF finalPoint = new PointF(finX, finY);
			//if current ands prev point are very close, don't add to path 
			if (VectorUtils.distance(finalPoint, prevPoint) > 0.1)
			{
				points.add(finalPoint);
				
			}
			
			//mv.addLabeledPoint(finalPoint, count + "");
			prevPoint = finalPoint;
			
			Log.d("PATHFINDING", "FINAL POINT CHOSEN: " + finalPoint.toString());
			//Log.d("PATHFINDING", usedPoint.toString());
			Log.d("PATHFINDING", count + "");
			
			//recalculate intersections between solution of prev intersection and destination 
			intersections = map.calculateIntersections(finalPoint, end);			
			
			count++;
		}
		
		points.add(end);
		int top = 0, bot = points.size();
		
		for (int i = points.size() - 2 ; i > 0 ; i--)
		{
			for (int j = i - 1 ; j >= 0 ; j--)
			{
				if (map.calculateIntersections(points.get(i), points.get(j)).isEmpty())
				{
					if (i > top)
					{
						top = i;
					}
					if (j < bot)
					{
						bot = j;
					}
				}
			}
		}
		
		Log.d("PATHFINDER", "HIGH REMOVE VAL: " + top);
		Log.d("PATHFINDER", "LOW REMOVE VAL: " + bot);
		/**
		for (int k = top - 1 ; k > bot; k--)
		{
			Log.d("PATHFINDER", "I MADE IT HERE, I REMOVED " + k + " AT POINT" + points.get(k).toString());
			points.remove(k);
			
		}
		
		*/
		/**
		int counter = 1;
		for (PointF point : points)
		{
			mv.addLabeledPoint(point, counter + "");
		}
		*/
		
	}
	
	public void removeBetween(List<PointF> list, int bot, int top)
	{
		
		
	}
}
