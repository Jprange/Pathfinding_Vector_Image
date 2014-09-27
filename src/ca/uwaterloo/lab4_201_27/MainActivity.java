package ca.uwaterloo.lab4_201_27;

import java.util.Arrays;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity
{
	
	TextView		orientation;
	
	TextView		xDisplacement;
	TextView		yDisplacement;
	TextView		Steps;
	TextView		State;
	TextView		completeMessage;
	
	MapView			mv;
	
	LineGraphView	graph;
	
	Movement		locCalc;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		LinearLayout tv = (LinearLayout) findViewById(R.id.label2);
		tv.setOrientation(LinearLayout.VERTICAL);
		
		graph = new LineGraphView(getApplicationContext(), 100, Arrays.asList("x", "y", "z"));
		// tv.addView(graph);
		// graph.setVisibility(View.VISIBLE);
		
		orientation = new TextView(getApplicationContext());
		
		xDisplacement = new TextView(getApplicationContext());
		yDisplacement = new TextView(getApplicationContext());
		Steps = new TextView(getApplicationContext());
		State = new TextView(getApplicationContext());
		completeMessage = new TextView(getApplicationContext());
		
		mv = new MapView(getApplicationContext(), 2000, 2000, 30, 30);
		registerForContextMenu(mv);
		
		
		
		
		
		tv.addView(orientation);
		// tv.addView(yDisplacement);
		// tv.addView(xDisplacement);
		tv.addView(Steps);
		tv.addView(completeMessage);
		// tv.addView(State);
		tv.addView(mv);
		
		NavigationalMap map = MapLoader.loadMap(getExternalFilesDir(null), "Lab-room.svg");
		mv.setMap(map);
		
		locCalc = new Movement(orientation, graph, xDisplacement, yDisplacement, Steps, State, mv, map, completeMessage);
		
		mv.addListener(locCalc);
		
		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		Sensor linAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		
		sensorManager.registerListener(locCalc, accelSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(locCalc, linAccelSensor, SensorManager.SENSOR_DELAY_GAME);
		
		sensorManager.registerListener(locCalc, magnetSensor, SensorManager.SENSOR_DELAY_GAME);
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		mv.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		return super.onContextItemSelected(item) || mv.onContextItemSelected(item);
	}
	
	
	public void simStep(View view)
	{
		locCalc.stepFunction();
	}
}
