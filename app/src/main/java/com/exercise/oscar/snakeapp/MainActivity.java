package com.exercise.oscar.snakeapp;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
public class MainActivity extends Activity {

    private SnakeApp grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the pixel dimensions of the screen
        Display display = getWindowManager().getDefaultDisplay();

        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);

        // Create a new instance of the SnakeEngine class
        grid = new SnakeApp(this, size);

        // Make snakeEngine the view of the Activity
        setContentView(grid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        grid.resume();
    }

    // Stop the thread in snakeEngine
    @Override
    protected void onPause() {
        super.onPause();
        grid.pause();
    }
}
