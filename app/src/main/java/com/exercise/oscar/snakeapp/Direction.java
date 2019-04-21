package com.exercise.oscar.snakeapp;

public enum Direction {
    UP(0),
    RIGHT(1),
    DOWN(2),
    LEFT(3);

    private int directionCode;

    Direction(int directioncode) {
        this.directionCode = directioncode;
    }

    public int generarDirection(){
        int random=(int)(Math.random()*3);
        while (random==this.directionCode){
            random=(int)(Math.random()*3);
        }
        return random;
    }
}
