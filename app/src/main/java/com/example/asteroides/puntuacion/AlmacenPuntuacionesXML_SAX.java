package com.example.asteroides.puntuacion;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.List;

public class AlmacenPuntuacionesXML_SAX implements AlmacenPuntuaciones {

    private static String FICHERO = "puntuaciones.xml";
    private Context contexto;
    private ListaPuntuaciones lista;
    private boolean cargadaLista;

    public AlmacenPuntuacionesXML_SAX(Context contexto) {
        this.contexto = contexto;
        lista = new ListaPuntuaciones();
        cargadaLista = false;
    }

    @Override
    public void guardarPuntuacion(int puntos, String nombre, long fecha) {
        try {
            if (!cargadaLista) {
                lista.leerXML(contexto.openFileInput(FICHERO));
                cargadaLista = true;
            }
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            Log.e("Asteroides", e.getMessage(), e);
        }
        lista.nuevo(puntos, nombre, fecha);
        try {
            lista.escribirXML(contexto.openFileOutput(FICHERO, Context.MODE_PRIVATE));
        } catch (Exception e) {
            Log.e("Asteroides", e.getMessage(), e);
        }
    }

    @Override
    public List<String> listaPuntuaciones(int cantidad) {
        try {
            if (!cargadaLista) {
                lista.leerXML(contexto.openFileInput(FICHERO));
            }
        } catch (Exception e) {
            Log.e("Asteroides", e.getMessage(), e);
        }
        return lista.aListString();
    }
}
