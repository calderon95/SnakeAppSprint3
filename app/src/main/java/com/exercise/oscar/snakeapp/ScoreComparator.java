package com.exercise.oscar.snakeapp;

import java.util.Comparator;

/*NUEVO*/
public class ScoreComparator implements Comparator<Jugador> {
    public int compare(Jugador score1, Jugador score2) {

        int sc1 = score1.getScore();
        int sc2 = score2.getScore();

        if (sc1 > sc2){
            return -1;
        }else if (sc1 < sc2){
            return +1;
        }else{
            return 0;
        }
    }
}