package com.example.asteroides;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Vector;

public class VistaJuego extends View implements SensorEventListener {

    public class ThreadJuego extends Thread {

        private boolean pausa, corriendo;

        public synchronized void pausar() {
            pausa = true;
        }

        public synchronized void reanudar() {
            pausa = false;
            notify();
        }

        public void detener() {
            corriendo = false;
            if (pausa) reanudar();
        }

        @Override
        public void run() {
            corriendo = true;
            while (corriendo) {
                actualizaFisica();
                synchronized (this) {
                    while (pausa)
                        try {
                            wait();
                        } catch (Exception e) {
                        }
                }
            }
        }
    }

    // //// NAVE //////
    private Grafico nave;// Gráfico de la nave
    private int giroNave; // Incremento de dirección
    private float aceleracionNave; // aumento de velocidad
    private static final int MAX_VELOCIDAD_NAVE = 20;
    // Incremento estándar de giro y aceleración
    private static final int PASO_GIRO_NAVE = 5;
    private static final float PASO_ACELERACION_NAVE = 0.5f;

    // //// MISIL //////
    private Grafico misil;
    private static int PASO_VELOCIDAD_MISIL = 12;
    private boolean misilActivo = false;
    private int tiempoMisil;

    private float mX = 0, mY = 0;
    private boolean disparo = false;

    // //// ASTEROIDES //////
    private Vector<Grafico> Asteroides; // Vector con los Asteroides
    private int numAsteroides = 5; // Número inicial de asteroides
    private int numFragmentos = 3; // Fragmentos en que se divide
    private Drawable drawableAsteroide[] = new Drawable[3];

    // //// THREAD Y TIEMPO //////
    // Thread encargado de procesar el juego
    private ThreadJuego thread = new ThreadJuego();
    // Cada cuanto queremos procesar cambios (ms)
    private static int PERIODO_PROCESO = 50;
    // Cuando se realizó el último proceso
    private long ultimoProceso = 0;

    private boolean hayValorInicial = false;
    private float valorInicial;

    SensorManager mSensorManager;

    // //// MULTIMEDIA //////
    SoundPool soundPool;
    int idDisparo, idExplosion;

    // //// PUNTUACIONES //////
    private int puntuacion = 0;
    private Activity padre;

    public VistaJuego(Context context, AttributeSet attrs) {
        super(context, attrs);

        Drawable drawableNave, drawableMisil;

        drawableNave = context.getResources().getDrawable(R.drawable.nave);
        nave = new Grafico(this, drawableNave);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (pref.getString("graficos", "1").equals("0")) {
            Path pathAsteroide = new Path();
            pathAsteroide.moveTo((float) 0.3, (float) 0.0);
            pathAsteroide.lineTo((float) 0.6, (float) 0.0);
            pathAsteroide.lineTo((float) 0.6, (float) 0.3);
            pathAsteroide.lineTo((float) 0.8, (float) 0.2);
            pathAsteroide.lineTo((float) 1.0, (float) 0.4);
            pathAsteroide.lineTo((float) 0.8, (float) 0.6);
            pathAsteroide.lineTo((float) 0.9, (float) 0.9);
            pathAsteroide.lineTo((float) 0.8, (float) 1.0);
            pathAsteroide.lineTo((float) 0.4, (float) 1.0);
            pathAsteroide.lineTo((float) 0.0, (float) 0.6);
            pathAsteroide.lineTo((float) 0.0, (float) 0.2);
            pathAsteroide.lineTo((float) 0.3, (float) 0.0);

            for (int i = 0; i < 3; i++) {
                ShapeDrawable dAsteroide = new ShapeDrawable(new PathShape(pathAsteroide, 1, 1));
                dAsteroide.getPaint().setColor(Color.WHITE);
                dAsteroide.getPaint().setStyle(Paint.Style.STROKE);
                dAsteroide.setIntrinsicWidth(50 - i * 14);
                dAsteroide.setIntrinsicHeight(50 - i * 14);
                drawableAsteroide[i] = dAsteroide;
            }
            setBackgroundColor(Color.BLACK);

            ShapeDrawable dMisil = new ShapeDrawable(new RectShape());
            dMisil.getPaint().setColor(Color.WHITE);
            dMisil.getPaint().setStyle(Paint.Style.STROKE);
            dMisil.setIntrinsicWidth(15);
            dMisil.setIntrinsicHeight(3);
            drawableMisil = dMisil;

            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } else {
            drawableAsteroide[0] = ContextCompat.getDrawable(context, R.drawable.asteroide1);
            drawableAsteroide[1] = ContextCompat.getDrawable(context, R.drawable.asteroide2);
            drawableAsteroide[2] = ContextCompat.getDrawable(context, R.drawable.asteroide3);
            drawableMisil = context.getResources().getDrawable(R.drawable.misil1);

            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        misil = new Grafico(this, drawableMisil);

        Asteroides = new Vector();
        for (int i = 0; i < numAsteroides; i++) {
            Grafico asteroide = new Grafico(this, drawableAsteroide[0]);
            asteroide.setIncY(Math.random() * 4 - 2);
            asteroide.setIncX(Math.random() * 4 - 2);
            asteroide.setAngulo((int) (Math.random() * 360));
            asteroide.setRotacion((int) (Math.random() * 8 - 4));
            Asteroides.add(asteroide);
        }

        activaSensores(context);

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        idDisparo = soundPool.load(context, R.raw.disparo, 0);
        idExplosion = soundPool.load(context, R.raw.explosion, 0);
    }

    @Override
    protected void onSizeChanged(int ancho, int alto, int ancho_anter, int alto_anter) {
        super.onSizeChanged(ancho, alto, ancho_anter, alto_anter);

        // Posicionamos la nave en el centro
        nave.setCenX((ancho - nave.getAncho()) / 2);
        nave.setCenY((alto - nave.getAlto()) / 2);

        // Una vez que conocemos nuestro ancho y alto.
        for (Grafico asteroide : Asteroides) {
            // Con el bucle do-while nos aseguramos que los asteroidez no aparezcan muy cerca de la nave
            do {
                asteroide.setCenX((int) Math.random() * (ancho - asteroide.getAncho()));
                asteroide.setCenY((int) Math.random() * (alto - asteroide.getAlto()));
            } while (asteroide.distancia(nave) < (ancho + alto) / 5);
        }

        ultimoProceso = System.currentTimeMillis();
        thread.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (Asteroides) {
            for (Grafico asteroide : Asteroides) {
                asteroide.dibujaGrafico(canvas);
            }
        }

        nave.dibujaGrafico(canvas);

        if (misilActivo == true)
            misil.dibujaGrafico(canvas);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float valor = sensorEvent.values[1];

        if (!hayValorInicial) {
            valorInicial = valor;
            hayValorInicial = true;
        }

        giroNave = (int) (valor - valorInicial) / 3;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void actualizaFisica() {
        long ahora = System.currentTimeMillis();

        if (ultimoProceso + PERIODO_PROCESO > ahora) {
            return; // Salir si el período de proceso no se ha cumplido
        }

        // Para una ejecución en tiempo real calculamos retardo
        double factorMov = (ahora - ultimoProceso) / PERIODO_PROCESO;
        ultimoProceso = ahora; // Para la próxima vez
        // Actualizamos velocidad y dirección de la nave a partir de
        // giroNave y aceleracionNave (según la entrada del jugador)
        nave.setAngulo((int) (nave.getAngulo() + giroNave * factorMov));
        double nIncX = nave.getIncX() + aceleracionNave * Math.cos(Math.toRadians(nave.getAngulo())) * factorMov;
        double nIncY = nave.getIncY() + aceleracionNave * Math.sin(Math.toRadians(nave.getAngulo())) * factorMov;
        // Actualizamos si el módulo de la velocidad no excede el máximo
        if (Math.hypot(nIncX, nIncY) <= MAX_VELOCIDAD_NAVE) {
            nave.setIncX(nIncX);
            nave.setIncY(nIncY);
        }
        // Actualizamos posiciones X e Y
        nave.incrementaPos(factorMov);
        synchronized (Asteroides) {
            for (Grafico asteroide : Asteroides) {
                asteroide.incrementaPos(factorMov);
            }
        }

        // Actualizamos posición de misil
        if (misilActivo) {

            misil.incrementaPos(factorMov);
            tiempoMisil -= factorMov;
            if (tiempoMisil < 0) {
                misilActivo = false;
            } else {
                for (int i = 0; i < Asteroides.size(); i++)
                    if (misil.verificaColision(Asteroides.elementAt(i))) {
                        destruyeAsteroide(i);
                        break;
                    }
            }
        }

        for (Grafico asteroide : Asteroides) {
            if (asteroide.verificaColision(nave))
                salir();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                disparo = true;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - mX);
                float dy = Math.abs(y - mY);
                if (dy < 6 && dx > 6) {
                    giroNave = Math.round((x - mX) / 2);
                    disparo = false;
                } else if (dx < 6 && dy > 6) {
                    aceleracionNave = Math.round((mY - y) / 25);
                    disparo = false;
                }
                break;

            case MotionEvent.ACTION_UP:
                giroNave = 0;
                aceleracionNave = 0;
                if (disparo) {
                    activaMisil();
                }
                break;
        }
        mX = x;
        mY = y;

        return true;
    }

    @Override
    public boolean onKeyDown(int codigoTecla, KeyEvent evento) {

        super.onKeyDown(codigoTecla, evento);

        // Suponemos que vamos a procesar la pulsación

        boolean procesada = true;

        switch (codigoTecla) {

            case KeyEvent.KEYCODE_DPAD_UP:

                aceleracionNave = +PASO_ACELERACION_NAVE;
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:

                giroNave = -PASO_GIRO_NAVE;
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:

                giroNave = +PASO_GIRO_NAVE;
                break;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:

                activaMisil();
                break;

            default:

                // Si estamos aquí, no hay pulsación que nos interese
                procesada = false;

                break;
        }

        return procesada;
    }

    @Override

    public boolean onKeyUp(int codigoTecla, KeyEvent evento) {

        super.onKeyUp(codigoTecla, evento);

        // Suponemos que vamos a procesar la pulsación

        boolean procesada = true;

        switch (codigoTecla) {

            case KeyEvent.KEYCODE_DPAD_UP:

                aceleracionNave = 0;
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:

                giroNave = 0;
                break;

            default:

                // Si estamos aquí, no hay pulsación que nos interese
                procesada = false;
                break;
        }

        return procesada;
    }

    private void destruyeAsteroide(int i) {

        int tam;
        if (Asteroides.get(i).getDrawable() != drawableAsteroide[2]) {
            if (Asteroides.get(i).getDrawable() == drawableAsteroide[1]) {
                tam = 2;
            } else {
                tam = 1;
            }
            for (int n = 0; n < numFragmentos; n++) {
                Grafico asteroide = new Grafico(this, drawableAsteroide[tam]);
                asteroide.setCenX(Asteroides.get(i).getCenX());
                asteroide.setCenY(Asteroides.get(i).getCenY());
                asteroide.setIncX(Math.random() * 7 - 2 - tam);
                asteroide.setIncY(Math.random() * 7 - 2 - tam);
                asteroide.setAngulo((int) (Math.random() * 360));
                asteroide.setRotacion((int) Math.random() * 8 - 4);
                Asteroides.add(asteroide);
            }
        }

        synchronized (Asteroides) {
            soundPool.play(idExplosion, 1, 1, 0, 0, 1);
            Asteroides.remove(i);
            misilActivo = false;
        }

        puntuacion += 1000;

        if (Asteroides.isEmpty()) {
            salir();
        }
    }

    private void activaMisil() {

        soundPool.play(idDisparo, 1, 1, 1, 0, 1);

        misil.setCenX(nave.getCenX());
        misil.setCenY(nave.getCenY());
        misil.setAngulo(nave.getAngulo());

        misil.setIncX(Math.cos(Math.toRadians(misil.getAngulo())) * PASO_VELOCIDAD_MISIL);
        misil.setIncY(Math.sin(Math.toRadians(misil.getAngulo())) * PASO_VELOCIDAD_MISIL);

        tiempoMisil = (int) Math.min(this.getWidth() / Math.abs(misil.getIncX()), this.getHeight() / Math.abs(misil.getIncY())) - 2;

        misilActivo = true;
    }

    public ThreadJuego getThread() {
        return thread;
    }

    public void activaSensores(Context context) {

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> listSensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);

        if (!listSensors.isEmpty()) {
            Sensor orientationSensor = listSensors.get(0);
            mSensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_GAME);
        }

    }

    public void desactivarSensores() {
        mSensorManager.unregisterListener(this);
    }

    public void setPadre(Activity padre) {
        this.padre = padre;
    }

    private void salir() {
        Bundle bundle = new Bundle();
        bundle.putInt("puntuacion", puntuacion);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        padre.setResult(Activity.RESULT_OK, intent);
        padre.finish();
    }
}