package com.exercise.oscar.snakeapp;

import java.io.Serializable;

/*NUEVO*/
public class Jugador implements Serializable {

    private String nombre;
    private int score;

    public Jugador(String nombre, int score){
        this.nombre = nombre;
        this.score = score;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return  nombre  + ", score: " + score + "\n";
    }
}
