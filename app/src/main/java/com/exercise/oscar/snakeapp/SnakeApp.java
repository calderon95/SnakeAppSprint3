package com.exercise.oscar.snakeapp;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;


class SnakeApp extends SurfaceView implements Runnable {


    // Utilizamos un solo thread para la aplicación
    private Thread thread = null;

    // Context es una referencia al MainActivity
    private Context context;

    // Para usar efectos de sonido
    private SoundPool soundPool;
    private MediaPlayer mediaPlayer;
    private int sound_comer = -1;
    private int sound_crash = -1;

    //Direccion de la cabeza, inicia el movimiento hacia la derecha
    private Direction direction = Direction.RIGHT;
    private Direction foodDirection = Direction.DOWN;

    //Dirección de la segunda serpiente
    private Direction snake2Direction;

    // Controla el tiempo de juego
    private long timestart;
    private long time;

    // Usamos un Nodo como comida
    private Node food;
    private Node poisonFood;

    private Node portal_1, portal_2;


    // Longitud X e Y de la pantalla actual
    private int pantallaX;
    private int pantallaY;

    // Número de bloques que se divide el ancho de la pantalla
    private final int numBloquesX = 70;
    // Número de bloques que se divide el largo de la pantalla
    private int numBloquesY;

    // Tamaño de los bloques en los que se dividirá la pantalla
    private int tamBloque;

    // Limites X e Y de la zona jugable
    private int limiteX_izq;
    private int limiteX_dch;
    private int limiteY_sup;
    private int limiteY_inf;


    // Controla el tiempo de pausa entre updates
    private long nextFrameTime;
    private long nextFoodDirectionTime;

    //nueva implementacion para 2 serpiente
    private long snake2Change;


    // Actualiza el juego 10 veces por segundo
    private final long FPS = 10;

    // 1 segundo = 1000millis
    private final long MILLIS_PER_SECOND = 1000;

    // Ranking
    private int contador;
    private ArrayList<Jugador> rankingJugador;

    // Puntuacion del jugador
    private int puntuacion;
    private boolean continuar = false;


    //Temporizadores
    private long timerSnake,timerPoisonFood,timerSong;

    // Valor para saber que canción se debe tocar
    private int disco = 0;


    // Localización del snake en la cuadrícula en todas sus partes, cada Nodo es una parte de la serpiente
    private LinkedList<Node> snake;
    //Localización de la segunda serpiente
    private LinkedList<Node> snake2;


    // Booleanos para controlar momentos de la partida
    private volatile boolean estaJugando;
    private volatile boolean estaComidaVenenosa;
    private volatile boolean estasegundaserpiente;
    private volatile boolean chochaConPortal_1;
    private volatile boolean chochaConPortal_2;

    // Lienzo para pintar el fondo, snake y la comida
    private Canvas canvas;

    // Necesario para usar el lienzo
    private SurfaceHolder surfaceHolder;

    // Para pintar sobre el lienzo
    private Paint paint;


    public SnakeApp(Context context, Point size) {
        super(context);

        this.context = context;

        //Se insertan los tamaños del dispositivo
        pantallaX = size.x;
        pantallaY = size.y;

        contador = 0;
        rankingJugador = new ArrayList<>();
        // Se calcula los pixeles que tendrá cada bloque basandonos en los bloques que queremos que tenga de ancho
        tamBloque = pantallaX / numBloquesX;
        // Se calcula en base al tamaño en pixeles de cada bloque, cuantos bloques hay en el largo de la pantalla
        numBloquesY = pantallaY / tamBloque;


        // Limites de la zona jugable
        limiteX_izq = numBloquesX - ((int) (numBloquesX * 0.93));
        limiteX_dch = numBloquesX - ((int) (numBloquesX * 0.05));
        limiteY_sup = numBloquesY - ((int) (numBloquesY * 0.9));
        limiteY_inf = numBloquesY - ((int) (numBloquesY * 0.1));


        // Preparacion de efectos de sonido
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            // Se crean las clases necesarias para manejarlos

            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Se preparan los sonidos en memoria
            descriptor = assetManager.openFd("EatSound.wav");
            sound_comer = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("Death_sound.wav");
            sound_crash = soundPool.load(descriptor, 0);


        } catch (IOException e) {
            // Error
        }

        //Iniciación de la música de fondo
        mediaPlayer = new MediaPlayer();
        mediaPlayer = MediaPlayer.create(context, R.raw.you_spin_me_round);
        mediaPlayer.start();

        //Temporizador de cambio de canción
        timerSong = System.currentTimeMillis();


        // Se inician los objetos para pintar
        surfaceHolder = getHolder();
        paint = new Paint();
        estaComidaVenenosa = false;

        // Inicia el juego
        newGame();
    }

    @Override
    public void run() {

        while (estaJugando) {

            // Actualiza 10 veces por segundo
            if (updateRequired()) {
                update();
                draw();
            }

        }
    }


    //Pausa el juego
    public void pause() {
        estaJugando = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }


    //Reanuda el juego
    public void resume() {
        estaJugando = true;
        thread = new Thread(this);
        thread.start();
    }


    public void newGame() {


        snake = new LinkedList<>();

        //Iniciamos el tiempos de juego y aparición de la segunda serpiente y de la comida venenosa
        timestart = System.currentTimeMillis();
        timerSnake = System.currentTimeMillis();
        timerPoisonFood = System.currentTimeMillis();


        direction = Direction.RIGHT;

        // Se empieza con una serpiente de longitud 3 (No queremos que sea una cabeza flotante la pobre)
        for (int i = 0; i < 3; i++) {
            int x = (numBloquesX / 2) - i;
            int y = (numBloquesY / 2);
            snake.add(new Node(x, y));
        }

        // Se añade la comida, la comida venenosa y los portales
        spawnFood();
        spawnPoisonFood();
        spawnPortal();
        chochaConPortal_1 = false;
        chochaConPortal_2 = false;

        estasegundaserpiente=false;

        // Resetea la puntuación
        puntuacion = 0;

        // Se prepara el siguiente tiempo para que salte el update
        nextFrameTime = System.currentTimeMillis();
    }

    //Metodo de aparición de la segunda serpiente
    public void AparicionSerp2(){
        snake2 = new LinkedList<>();

        snake2Direction = Direction.LEFT;

        // Se empieza con una serpiente de longitud 3
        for (int j = 0; j < 3; j++) {
            int x = (numBloquesX / 2);
            int y = (numBloquesY / 2) - j;
            snake2.add(new Node(x, y));
        }
        estasegundaserpiente=true;
    }

    // Comprobación de que toque los limites
    public void EstadoSerp2(){
        if (snake2.getFirst().getX() == (limiteX_izq - 1)) estasegundaserpiente = false;
        if (snake2.getFirst().getX() >= (limiteX_dch - 1)) estasegundaserpiente = false;
        if (snake2.getFirst().getY() == (limiteY_sup - 1)) estasegundaserpiente = false;
        if (snake2.getFirst().getY() == (limiteY_inf - 1)) estasegundaserpiente = false;
    }


    // Aparición de comida
    public void spawnFood() {

        // La comida aparece en una zona aleatoria dentro de la zona jugable y además lejos del limite de la zona (para jugar con más facilidad)

        food = new Node();
        Random random = new Random();
        int X = random.nextInt((limiteX_dch - 3) - limiteX_izq) + limiteX_izq + 1;
        int Y = random.nextInt((limiteY_inf - 3) - limiteY_sup) + limiteY_sup + 1;
        food.setX(X);
        food.setY(Y);

    }

    // Aparición de los portales
    public void spawnPortal() {
        Random random = new Random();
        int x1 = this.limiteX_izq + 6;
        int x2 = this.limiteX_dch - 6;
        int y1 = random.nextInt((limiteY_inf - 3) - limiteY_sup) + limiteY_sup + 1;
        int y2 = random.nextInt((limiteY_inf - 3) - limiteY_sup) + limiteY_sup + 1;
        portal_1 = new Node(x1, y1);
        portal_2 = new Node(x2, y2);
    }

    // Aparición de la comida venenosa
    public void spawnPoisonFood() {

        // La comida aparece en una zona aleatoria dentro de la zona jugable y además lejos del limite de la zona (para jugar con más facilidad)

        this.poisonFood = new Node();
        Random random = new Random();
        int X = this.limiteX_izq;
        int Y = random.nextInt((limiteY_inf - 3) - limiteY_sup) + limiteY_sup + 1;
        poisonFood.setX(X);
        poisonFood.setY(Y);
        estaComidaVenenosa = true;

    }

    //Metodo para comer
    private void eatFood(Node Last) {

        // Se come la comida y crece uno de longitud
        snake.addLast(Last);

        // Se añade comida
        spawnFood();
        // Se aumenta en uno la puntación
        puntuacion = puntuacion + 500;
        // Se utiliza el efecto de sonido para comer
        soundPool.play(sound_comer, 1, 1, 0, 0, 1);



    }

    //Metodo para mover la serpiente
    private Node moveSnake(LinkedList<Node> snake,Direction direction) {
        // Se mueve el cuerpo
        Node first = snake.getFirst();

        // Se mueve la cabeza hacia la direccion elegida
        switch (direction) {
            case UP:
                snake.addFirst(new Node(first.getX(), first.getY() + 1));
                break;

            case RIGHT:
                snake.addFirst(new Node(first.getX() + 1, first.getY()));
                break;

            case DOWN:
                snake.addFirst(new Node(first.getX(), first.getY() - 1));
                break;

            case LEFT:
                snake.addFirst(new Node(first.getX() - 1, first.getY()));
                break;
        }
        Node Last = snake.getLast();
        snake.removeLast();
        return Last;
    }

    // Metodo para transportarse entre portales
    private void entraPortal(Node portal_1, Node portal_2) {
        switch (direction) {
            case UP:
                snake.addFirst(new Node(portal_1.getX(), portal_1.getY() + 1));
                break;

            case RIGHT:
                snake.addFirst(new Node(portal_1.getX() + 1, portal_1.getY()));
                break;

            case DOWN:
                snake.addFirst(new Node(portal_1.getX(), portal_1.getY() - 1));
                break;

            case LEFT:
                snake.addFirst(new Node(portal_1.getX() - 1, portal_1.getY()));
                break;
        }
        Node Last = snake.getLast();
        snake.removeLast();
        if (Last.getX() == portal_2.getX() && Last.getY() == portal_2.getY()) {
            chochaConPortal_1 = false;
            chochaConPortal_2 = false;
        }
    }

    // Metodo que se mueva la comida
    private void moveFood() {
        // Se mueve el cuerpo
        int x = food.getX();
        int y = food.getY();
        // Se mueve la cabeza hacia la direccion elegida
        switch (foodDirection) {
            case UP:
                food.setY(food.getY() + 1);
                if (food.getY() == this.limiteY_inf - 1 || food.getY() == this.limiteY_sup - 1) {
                    food.setX(x);
                    food.setY(y);
                    foodDirection = Direction.DOWN;
                }
                break;

            case RIGHT:
                food.setX(food.getX() + 1);
                if (food.getX() == this.limiteX_dch - 1 || food.getX() == this.limiteX_izq - 1) {
                    food.setX(x);
                    food.setY(y);
                    foodDirection = Direction.LEFT;
                }
                break;

            case DOWN:
                food.setY(food.getY() - 1);
                if (food.getY() == this.limiteY_inf - 1 || food.getY() == this.limiteY_sup - 1) {
                    food.setX(x);
                    food.setY(y);
                    foodDirection = Direction.UP;
                }
                break;

            case LEFT:
                food.setX(food.getX() - 1);
                if (food.getX() == this.limiteX_dch - 1 || food.getX() == this.limiteX_izq - 1) {
                    food.setX(x);
                    food.setY(y);
                    foodDirection = Direction.RIGHT;
                }
                break;
        }

    }

    // Movimiento de la comida venenosa
    private void moveVenenoFood() {
        poisonFood.setX(poisonFood.getX() + 1);
        if (poisonFood.getX() == this.limiteX_dch - 1) {
            estaComidaVenenosa = false;
        }
    }

    // Ordenación de ranking
    private void sort() {
        ScoreComparator comparator = new ScoreComparator();
        Collections.sort(rankingJugador, comparator);
    }

    // Muestra el ranking en un mensaje Toast
    public static void backgroundToast(final Context context, final String msg) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Actualización del juego
    public void update() {

        //Toca portales
        if (snake.getFirst().getX() == portal_1.getX() && snake.getFirst().getY() == portal_1.getY()) {
            chochaConPortal_1 = true;
        }
        if (snake.getFirst().getX() == portal_2.getX() && snake.getFirst().getY() == portal_2.getY()) {
            chochaConPortal_2 = true;
        }

        //Se transporta al otro portal
        if (chochaConPortal_1) {

            entraPortal(portal_2, portal_1);
            moveVenenoFood();
            moveFood();

        } else if (chochaConPortal_2) {

            entraPortal(portal_1, portal_2);
            moveVenenoFood();
            moveFood();

        } else {

            // Comprobacion de si la cabeza de la serpiente choca contra algún elemento
            Node Last = moveSnake(snake,direction);

            if (estasegundaserpiente) {
                moveSnake(snake2, snake2Direction);
                EstadoSerp2();
            }


            moveVenenoFood();

            if (snake.getFirst().getX() == food.getX() && snake.getFirst().getY() == food.getY()) {
                eatFood(Last);
            }

            moveFood();

            if (snake.getFirst().getX() == food.getX() && snake.getFirst().getY() == food.getY()) {
                eatFood(Last);
            }

            //Se actualiza el tiempo de juego
            time = System.currentTimeMillis() - timestart;
            time = TimeUnit.MILLISECONDS.toSeconds(time);

            //Tiempo de cambio de canción
            long timeSong = System.currentTimeMillis()-timerSong;
            timeSong = TimeUnit.MILLISECONDS.toSeconds(timeSong);

            // Cada 20 segundos cambia de canción en orden
            if(timeSong==20){
                switch (disco){
                    case 0:
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = MediaPlayer.create(context, R.raw.never_gonna_give_you_up);
                        mediaPlayer.start();
                        disco++;
                        timerSong+=TimeUnit.SECONDS.toMillis(timeSong);
                        break;
                    case 1:
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = MediaPlayer.create(context, R.raw.smoothcriminal);
                        mediaPlayer.start();
                        disco++;
                        timerSong+=TimeUnit.SECONDS.toMillis(timeSong);
                        break;

                    case 2:
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = MediaPlayer.create(context, R.raw.sweet_dreams);
                        mediaPlayer.start();
                        disco++;
                        timerSong+=TimeUnit.SECONDS.toMillis(timeSong);
                        break;

                    case 3:
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = MediaPlayer.create(context, R.raw.you_spin_me_round);
                        mediaPlayer.start();
                        disco=0;
                        timerSong+=TimeUnit.SECONDS.toMillis(timeSong);
                        break;
                }
            }


            //Si se muere vuelve a empezar una nueva partida
            if (detectarMuerte()) {

                contador++;

                // Se utiliza el efecto de sonido de muerte
                soundPool.play(sound_crash, 1, 1, 0, 0, 1);


                String ranking = "";

                rankingJugador.add(new Jugador("Snake" + contador, puntuacion));
                sort();
                if (rankingJugador.size() > 3) {
                    rankingJugador.remove(rankingJugador.size() - 1);
                }
                for (Jugador j : rankingJugador) {
                    ranking += j.toString();
                }


                backgroundToast(context, ranking);

                if(puntuacion>=500){
                    continuar = true;
                }
                pause();


            }

            // Cambio de direccion de comida
            if (foodupdateRequired()) {
                int random = foodDirection.generarDirection();
                switch (random) {
                    case 0:
                        foodDirection = Direction.UP;
                        break;
                    case 1:
                        foodDirection = Direction.RIGHT;
                        break;
                    case 2:
                        foodDirection = Direction.DOWN;
                        break;
                    case 3:
                        foodDirection = Direction.LEFT;
                        break;
                }
            }

            // Cambio de direccion de la segunda serpiente
            if (isChangeDirection()){
                int random=foodDirection.generarDirection();
                switch (random){
                    case 0:
                        snake2Direction=Direction.DOWN;
                        break;
                    case 1:
                        snake2Direction=Direction.LEFT;
                        break;
                    case 2:
                        snake2Direction=Direction.RIGHT;
                        break;
                    case 3:
                        snake2Direction=Direction.UP;
                        break;
                }
            }

            // Controla el spawn de la comida venenosa
            if (!estaComidaVenenosa) {
                if (isPoisonFood()) {
                    spawnPoisonFood();
                }
            }

            // Controla el spawn de la segunda serpiente
            if (!estasegundaserpiente){
                if (isSnake2Time()){
                    AparicionSerp2();
                }
            }

        }


    }

    private boolean detectarMuerte() {

        // Si muere la serpiente
        boolean dead = false;

        // Muere si toca alguna pared
        if (snake.getFirst().getX() == (limiteX_izq - 1)) dead = true;
        if (snake.getFirst().getX() >= (limiteX_dch - 1)) dead = true;
        if (snake.getFirst().getY() == (limiteY_sup - 1)) dead = true;
        if (snake.getFirst().getY() == (limiteY_inf - 1)) dead = true;

        // Muere si se come a si mismo o si entraPortal con la comida venenosa
        Node head = snake.pollFirst();
        for (Node point : snake) {
            if ((point.getX() == head.getX() && point.getY() == head.getY()) ||
                    (point.getX() == poisonFood.getX() && point.getY() == poisonFood.getY())) {
                dead = true;
            }
        }
        snake.addFirst(head);


        return dead;
    }

    public void draw() {

        // Se preparan los elementos que se van a pintar

        //Si la superficie es valida
        if (surfaceHolder.getSurface().isValid()) {

            // Se bloquea el lienzo hasta preparar lo necesario
            canvas = surfaceHolder.lockCanvas();

            // Se crea un rect que servirá como molde para pintar fondos, comida y serpiente
            Rect rect = new Rect(0, 0, pantallaX, pantallaY);

            // Prepara fondo exterior
            paint.setColor(Color.argb(255, 123, 255, 255));
            Bitmap bitmapJugable, bitmapEntorno,bitmapStart;
            bitmapEntorno = BitmapFactory.decodeResource(this.getResources(), R.drawable.color_entorno);

            // Prepara fondo jugable
            bitmapJugable = BitmapFactory.decodeResource(this.getResources(), R.drawable.pantalla_gameboy);

            //Boton start
            bitmapStart = BitmapFactory.decodeResource(this.getResources(), R.drawable.start);

            // Pinta fondo exterior
            canvas.drawBitmap(bitmapEntorno, null, rect, paint);

            // Cambio de molde y pinta fondo jugable
            rect.set((limiteX_izq * tamBloque), (limiteY_sup * tamBloque), ((limiteX_dch - 1) * tamBloque), ((limiteY_inf - 1) * tamBloque));
            canvas.drawBitmap(bitmapJugable, null, rect, paint);

            // Cambio de molde y pinta boton start
            rect.set(675, ((limiteY_inf-1) * tamBloque), 1150, pantallaY);
            canvas.drawBitmap(bitmapStart, null, rect, paint);

            // Poner color de marcador a blanco
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Se pone la escala del texto y se pinta
            paint.setTextSize(70);
            canvas.drawText("Puntuacion : " + puntuacion, 300, tamBloque * 3, paint);
            canvas.drawText("Tiempo : " + time, 1050, tamBloque * 3, paint);

            // Contador para pintar la cabeza y el cuerpo de manera distinta
            int i = 0;

            // Se prepara la creacion de la serpiente y su color
            for (Node point : snake) {
                // Se va pintando un bloque de la serpiente cada vez
                if (i == 0) {
                    // Pinta solo la cabeza
                    paint.setColor(Color.argb(255, 0, 100, 0));
                    canvas.drawRect(point.getX() * tamBloque,
                            (point.getY() * tamBloque),
                            (point.getX() * tamBloque) + tamBloque,
                            (point.getY() * tamBloque) + tamBloque,
                            paint);
                    i++;
                } else {
                    // Pinta el cuerpo
                    paint.setColor(Color.argb(255, 0, 0, 0));
                    canvas.drawRect(point.getX() * tamBloque,
                            (point.getY() * tamBloque),
                            (point.getX() * tamBloque) + tamBloque,
                            (point.getY() * tamBloque) + tamBloque,
                            paint);
                }
            }

            //Pinta la segunda serpiente
            if (estasegundaserpiente){
                for (Node point:snake2){
                    paint.setColor(Color.argb(255, 1, 1, 1));
                    canvas.drawRect(point.getX() * tamBloque,
                            (point.getY() * tamBloque),
                            (point.getX() * tamBloque) + tamBloque,
                            (point.getY() * tamBloque) + tamBloque,
                            paint);
                }
            }

            // Pinta la comida con el color deseado
            paint.setColor(Color.RED);
            canvas.drawRect(food.getX() * tamBloque,
                    (food.getY() * tamBloque),
                    (food.getX() * tamBloque) + tamBloque,
                    (food.getY() * tamBloque) + tamBloque,
                    paint);
            // Pinta la comida venenosa con el color deseado
            if (estaComidaVenenosa) {
                paint.setColor(Color.BLUE);
                canvas.drawRect(poisonFood.getX() * tamBloque,
                        (poisonFood.getY() * tamBloque),
                        (poisonFood.getX() * tamBloque) + tamBloque,
                        (poisonFood.getY() * tamBloque) + tamBloque,
                        paint);
            }

            //Pinta los portales
            paint.setColor(Color.MAGENTA);
            canvas.drawRect(portal_1.getX() * tamBloque,
                    (portal_1.getY() * tamBloque),
                    (portal_1.getX() * tamBloque) + tamBloque,
                    (portal_1.getY() * tamBloque) + tamBloque,
                    paint);

            canvas.drawRect(portal_2.getX() * tamBloque,
                    (portal_2.getY() * tamBloque),
                    (portal_2.getX() * tamBloque) + tamBloque,
                    (portal_2.getY() * tamBloque) + tamBloque,
                    paint);



            // Desbloquea el lienzo y revela lo que se ha pintado en este frame
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public boolean updateRequired() {

        // Actualiza el proximo frame si es necesario
        if (nextFrameTime <= System.currentTimeMillis()) {

            // Se prepara el siguiente tiempo en el que se debe actualizar
            nextFrameTime = System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;

            // Devuelve true para que se pueda ejecutar el update y el draw
            return true;
        }

        return false;
    }

    public boolean foodupdateRequired() {

        // Actualiza el proximo frame si es necesario
        if (nextFoodDirectionTime <= System.currentTimeMillis()) {

            // Se prepara el siguiente tiempo en el que se debe actualizar
            nextFoodDirectionTime = System.currentTimeMillis() + MILLIS_PER_SECOND * 10;

            // Devuelve true para que se pueda ejecutar el update y el draw
            return true;
        }

        return false;
    }

    //Controla el spawn de la comida venenosa
    public boolean isPoisonFood() {

        long timePoisonFood = System.currentTimeMillis()-timerPoisonFood;
        timePoisonFood = TimeUnit.MILLISECONDS.toSeconds(timePoisonFood);

        if(timePoisonFood==10){
            timerPoisonFood+=TimeUnit.SECONDS.toMillis(timePoisonFood);
            return true;
        }
        return false;
    }

    // Controla el spawn de la segunda serpiente
    public boolean isSnake2Time(){

        long timeSnake = System.currentTimeMillis()-timerSnake;
        timeSnake = TimeUnit.MILLISECONDS.toSeconds(timeSnake);

        if(timeSnake==10){
            timerSnake+=TimeUnit.SECONDS.toMillis(timeSnake);
            return true;
        }
        return false;

    }

    //Controla el cambio de dirección de la segunda serpiente
    public boolean isChangeDirection(){
        if (snake2Change <= System.currentTimeMillis()) {

            // Se prepara el siguiente tiempo en el que se debe actualizar
            snake2Change = System.currentTimeMillis() + 2000;

            // Devuelve true para que se pueda ejecutar el update y el draw
            return true;
        }

        return false;
    }


    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        // Se prepara el evento cuando se toca la pantalla
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            // Cuando se levanta el dedo
            case MotionEvent.ACTION_UP:
                // Si se ha pulsado en la mitad derecha  de la pantalla, se va cambiando la posición hacia la dcha.
                if (motionEvent.getX() >= pantallaX / 2) {

                    switch (direction) {
                        case UP:
                            direction = Direction.LEFT;
                            break;
                        case RIGHT:
                            direction = Direction.UP;
                            break;
                        case DOWN:
                            direction = Direction.RIGHT;
                            break;
                        case LEFT:
                            direction = Direction.DOWN;
                            break;


                    }
                    // Si se ha pulsado en la mited izquierda de la pantalla, se va cambiando la posición hacia la izq .

                } if((motionEvent.getX() < pantallaX / 2)) {
                    switch (direction) {
                        case UP:
                            direction = Direction.RIGHT;
                            break;
                        case LEFT:
                            direction = Direction.UP;
                            break;
                        case DOWN:
                            direction = Direction.LEFT;
                            break;
                        case RIGHT:
                            direction = Direction.DOWN;
                            break;
                    }
                }

            break;

            // Activa la nueva partida si se pulsa Start con una puntuación mínima
            case MotionEvent.ACTION_DOWN:
                if(continuar==true){
                    continuar=false;
                    if(motionEvent.getX() > 675 && motionEvent.getX() <  1150 && motionEvent.getY() < pantallaY && motionEvent.getY() > ((limiteY_inf-1) * tamBloque)){
                        /* 675, ((limiteY_inf-1) * tamBloque), 1150, pantallaY */
                        resume();
                        newGame();
                    }

                }
                break;
        }

        return true;
    }
}
