package ca.uwaterloo.lab4_201_27;

import java.util.ArrayList;
import java.util.List;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class Movement implements SensorEventListener, PositionListener
{
	
	private float[]			accelVals		= { (float) 0.0, (float) 0.0, (float) 0.0 };
	private float[]			magnetVals		= { (float) 0.0, (float) 0.0, (float) 0.0 };
	private float[]			linAccelVals	= { (float) 0.0, (float) 0.0, (float) 0.0 };
	
	private TextView		curActivity;
	
	private TextView		xDisplacement;
	private TextView		yDisplacement;
	private TextView		Steps;
	private TextView		State;
	private TextView		completeMessage;
	
	private LineGraphView	graph;
	
	private double			prevValue		= 0;
	private double			prevLow			= 0;
	
	private double			highVal			= 0;
	
	private double			lowVal			= 0;
	
	private int				steps			= 0;
	private int				state			= 0;
	
	private boolean			run				= true;
	
	private MapView			mv;
	private NavigationalMap	navMap;
	
	private double			xDistance		= 0;
	private double			yDistance		= 0;
	
	private float			curDirection;
	private float			directionCorrectFactor;
	
	private long			stepBaseLine;
	
	private PointF			origin;
	private PointF			curLoc;
	private PointF			destination;
	private List<PointF>	path;
	
	private Path			pathFinder;
	
	public Movement(TextView curActivity, LineGraphView graph, TextView xDistances, TextView yDistances, TextView Steps, TextView State,
			MapView mapView, NavigationalMap navMap, TextView complete)
	{
		this.curActivity = curActivity;
		mv = mapView;
		this.graph = graph;
		this.xDisplacement = xDistances;
		this.yDisplacement = yDistances;
		this.Steps = Steps;
		this.State = State;
		directionCorrectFactor = 0;
		stepBaseLine = SystemClock.elapsedRealtime();
		this.navMap = navMap;
		completeMessage = complete;
		pathFinder = new Path(navMap, Steps, mv);
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			accelVals = event.values;
		}
		else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			magnetVals = event.values;
		}
		else
		{
			linAccelVals = event.values;
		}
		
		float[] R = new float[9];
		float[] I = new float[9];
		float[] orientationVector = new float[3];
		
		SensorManager.getRotationMatrix(R, I, accelVals, magnetVals);
		orientationVector = SensorManager.getOrientation(R, orientationVector);
		
		curDirection = (float) (orientationVector[0]);
		curDirection -= directionCorrectFactor;
		
		double Cont = 0.25;
		double lowpass = Cont * Double.valueOf(linAccelVals[2]) + (1 - Cont) * prevLow;
		
		if (stepDetector(lowpass) && curLoc != null)
		{	
			stepFunction();
		}
		
		curActivity.setText("Current Heading: " + Float.toString(curDirection));
		
		xDisplacement.setText("East Displacement: " + Double.toString(xDistance));
		
		yDisplacement.setText("North Displacement: " + Double.toString(yDistance));
		
		Steps.setText("Number of Steps: " + Integer.toString(steps));
		
		State.setText("Current State: " + Integer.toString(state));
		
		float[] graphPoints = { (float) lowpass, 0/** linAccelVals[2] **/
		, 0 /** accelVals[2] **/
		};
		graph.addPoint(graphPoints);
		
	}
	
	public void stepFunction()
	{
		// code to add distance to x and y components
		if (curLoc != null && run)
		{
			
			double stepDistance = 0.5;
			
			xDistance = stepDistance * Math.cos(curDirection);
			yDistance = stepDistance * Math.sin(curDirection);
			steps++;
			
			PointF nextLoc = new PointF((float) (curLoc.x + xDistance), (float) (curLoc.y + yDistance));
			
			// if not in a wall
			if (navMap.calculateIntersections(curLoc, nextLoc).isEmpty())
			{
				curLoc = nextLoc;
				mv.setUserPoint(curLoc);
				if (destination != null)
				{
					mv.setUserPath(null);
					if (path != null)
					{
						path.clear();
					}
					pathFinder.createPath(curLoc, destination);
					path = pathFinder.getPoints();
					
					mv.setUserPath(path);
					
					if (VectorUtils.distance(curLoc, destination) < 0.75)
					{
						curLoc = destination;
						mv.setUserPoint(curLoc);
						completeMessage.setText("YOU HAVE ARRIVED");
						completeMessage.setTextSize(TypedValue.TYPE_STRING, 32);
						run = false;
						curActivity.setVisibility(View.INVISIBLE);
						Steps.setVisibility(View.INVISIBLE);
						mv.setVisibility(View.INVISIBLE);
					}
				}
				
				
			}
		}
	}
	
	public boolean stepDetector(double accelVal)
	{
		
		// Five States
		// State = 0 is waiting for a step to occur, machine begins in this
		// state, transitions if accel goes above 2.5. as well, the clock is
		// reset.
		// State = 1 is when a step is first being recorded, so the program
		// records the maximum value that occurs and save it
		// State = 2 is falling, when the program stops looking for new max
		// values and starts looking for min values
		// State = 3 is the same as 1, except it looks for min values and then
		// saves the min value
		// State = 4 is the final stage where it waits for accel to go +, and if
		// the difference between the min and max is between a certain range
		// the step counter returns true, and if the difference was too high or
		// too low, it discards the step
		
		// update the time between the start and end of a step cycle
		
		long stepDiff = SystemClock.elapsedRealtime() - stepBaseLine;
		long maxStepDiff = 1000;
		
		boolean step = false;
		
		if (state == 0) // State 0
		{
			if (accelVal >= 0.5)
			{
				state = 1;
				stepBaseLine = SystemClock.elapsedRealtime();
			}
		}
		else if (state == 1) // State 1
		{
			if (prevValue * 1.01 > accelVal)
			{
				highVal = prevValue;
				state = 2;
			}
		}
		else if (state == 2) // State 2
		{
			if (accelVal <= -0.5)
			{
				state = 3;
			}
		}
		else if (state == 3) // State 3
		{
			if (prevValue * 1.01 < accelVal)
			{
				lowVal = prevValue;
				state = 4;
			}
		}
		else if (state == 4) // State 4
		{
			if (accelVal > 0)
			{
				step = true;
				state = 0;
				highVal = 0;
				lowVal = 0;
			}
		}
		
		if (state != 0 && (stepDiff > maxStepDiff))
		{
			// reset baseline and state
			step = false;
			state = 0;
			stepBaseLine = SystemClock.elapsedRealtime();
			highVal = 0;
			lowVal = 0;
		}
		
		return step;
	}
	
	@Override
	public void originChanged(MapView source, PointF loc)
	{
		curLoc = loc;
		mv.setUserPoint(curLoc);
		
		if (destination != null)
		{
			mv.setUserPath(null);
			if (path != null)
			{
				path.clear();
			}
			pathFinder.createPath(curLoc, destination);
			path = pathFinder.getPoints();
			
			mv.setUserPath(path);
		}
		
		
	}
	
	@Override
	public void destinationChanged(MapView source, PointF dest)
	{		
		destination = dest;
		
		if (curLoc != null)
		{
			
			mv.setUserPath(null);
			if (path != null)
			{
				path.clear();
			}
			
			pathFinder.createPath(curLoc, destination);
			path = pathFinder.getPoints();
			
			mv.setUserPath(path);
		}
	}
	
	
	
}
