package com.example.asteroides;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.Vector;

public class VistaJuego extends View {

    // //// NAVE //////
    private Grafico nave;// Gráfico de la nave
    private int giroNave; // Incremento de dirección
    private float aceleracionNave; // aumento de velocidad
    private static final int MAX_VELOCIDAD_NAVE = 20;
    // Incremento estándar de giro y aceleración
    private static final int PASO_GIRO_NAVE = 5;
    private static final float PASO_ACELERACION_NAVE = 0.5f;

    // //// ASTEROIDES //////
    private Vector<Grafico> Asteroides; // Vector con los Asteroides
    private int numAsteroides = 5; // Número inicial de asteroides
    private int numFragmentos = 3; // Fragmentos en que se divide

    public VistaJuego(Context context, AttributeSet attrs) {
        super(context, attrs);

        Drawable drawableNave, drawableAsteroide, drawableMisil;

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
            ShapeDrawable dAsteroide = new ShapeDrawable(new PathShape(pathAsteroide, 1, 1));
            dAsteroide.getPaint().setColor(Color.WHITE);
            dAsteroide.getPaint().setStyle(Paint.Style.STROKE);
            dAsteroide.setIntrinsicWidth(50);
            dAsteroide.setIntrinsicHeight(50);
            drawableAsteroide = dAsteroide;
            setBackgroundColor(Color.BLACK);

            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } else {
            drawableAsteroide = ContextCompat.getDrawable(context, R.drawable.asteroide1);

            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        Asteroides = new Vector();
        for (int i = 0; i < numAsteroides; i++) {
            Grafico asteroide = new Grafico(this, drawableAsteroide);
            asteroide.setIncY(Math.random() * 4 - 2);
            asteroide.setIncX(Math.random() * 4 - 2);
            asteroide.setAngulo((int) (Math.random() * 360));
            asteroide.setRotacion((int) (Math.random() * 8 - 4));
            Asteroides.add(asteroide);
        }
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


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Grafico asteroide : Asteroides) {
            asteroide.dibujaGrafico(canvas);
        }

        nave.dibujaGrafico(canvas);
    }
}
