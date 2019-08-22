package com.example.asteroides.puntuacion;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Vector;

public class AlmacenPuntuacionesPreferencias implements AlmacenPuntuaciones {

    private static String PREFERENCIAS = "puntuaciones";
    private Context context;

    public AlmacenPuntuacionesPreferencias(Context context) {
        this.context = context;
    }

    public void guardarPuntuacion(int puntos, String nombre, long fecha) {
        SharedPreferences preferencias = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferencias.edit();
        editor.putString("puntuacion", puntos + " " + nombre);
        editor.commit();
    }

    public Vector<String> listaPuntuaciones(int cantidad) {
        Vector<String> result = new Vector<String>();
        SharedPreferences preferencias = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE);
        String s = preferencias.getString("puntuacion", "");
        if (!s.isEmpty()) {
            result.add(s);
        }
        return result;
    }
}
