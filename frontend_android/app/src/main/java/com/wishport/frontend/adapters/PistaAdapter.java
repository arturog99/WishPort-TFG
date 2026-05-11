package com.wishport.frontend.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wishport.frontend.R;
import com.wishport.frontend.api.RetrofitClient;
import com.wishport.frontend.models.Pista;

import java.util.List;

/**
 * ADAPTADOR DE PISTAS: Es el "traductor" entre la lista de objetos Pista y la pantalla.
 *
 * Un RecyclerView no sabe cómo dibujar una Pista. El adaptador le dice:
 * "para cada pista de la lista, infla este diseño XML y rellena sus campos".
 *
 * Patrón ViewHolder: Guarda referencias a las vistas de cada fila para
 * no tener que buscarlas en el XML cada vez que el usuario hace scroll.
 * Esto mejora mucho el rendimiento con listas largas.
 */
public class PistaAdapter extends RecyclerView.Adapter<PistaAdapter.PistaViewHolder> {

    /** Lista de pistas que se van a mostrar */
    private List<Pista> listaPistas;
    /** Referencia al "escuchador" que se avisa cuando el usuario pulsa una pista */
    private OnItemClickListener listener;

    /**
     * Interfaz de callback: permite que PistasActivity sepa cuándo
     * el usuario ha pulsado una pista, sin que el adaptador conozca la Activity.
     * Principio de desacoplamiento.
     */
    public interface OnItemClickListener {
        /** Se ejecuta cuando el usuario pulsa el item de una pista */
        void onItemClick(Pista pista);
    }

    /**
     * Registra el listener que recibirá los clics en las pistas.
     * Llamado desde PistasActivity después de crear el adaptador.
     * @param listener Objeto (normalmente la Activity) que implementa OnItemClickListener.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Constructor del adaptador.
     * @param listaPistas Lista de pistas que se mostrarán en el RecyclerView.
     */
    public PistaAdapter(List<Pista> listaPistas) {
        this.listaPistas = listaPistas;
    }

    /**
     * Crea la "cáscara" visual (ViewHolder) de una nueva fila del listado.
     * Solo se llama cuando se necesita una fila nueva (no cada vez que se hace scroll).
     * Infla el XML item_pista.xml y crea un PistaViewHolder con sus referencias.
     */
    @NonNull
    @Override
    public PistaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pista, parent, false);
        return new PistaViewHolder(view);
    }

    /**
     * Rellena los datos reales de una pista concreta en los elementos visuales.
     * Se llama cada vez que una fila entra en pantalla durante el scroll.
     * @param holder   ViewHolder con las referencias a los elementos del XML.
     * @param position Posición en la lista de pistas (0 = primera pista).
     */
    @Override
    public void onBindViewHolder(@NonNull PistaViewHolder holder, int position) {
        Pista pista = listaPistas.get(position);
        
        holder.tvNombrePista.setText(pista.getNombre());
        holder.tvDeporte.setText(pista.getDeporte());
        holder.tvEstado.setText("Estado: " + pista.getEstado());

        // Glide: librería que descarga y muestra imágenes de URL de forma asíncrona.
        // placeholder = imagen que se muestra mientras carga.
        // error = imagen que se muestra si la descarga falla.
        String fotoUrl = corregirUrl(pista.getFotoUrl());
        Glide.with(holder.itemView.getContext())
                .load(fotoUrl)
                .placeholder(R.drawable.ic_pista_placeholder)
                .error(R.drawable.ic_pista_error)
                .centerCrop()
                .into(holder.ivFotoPista);

        // Cuando el usuario pulsa cualquier parte del item, notificamos al listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pista);
            }
        });
    }

    /**
     * Construye la URL completa de la imagen.
     * El servidor puede devolver rutas relativas (ej: "/images/pista1.jpg") o absolutas.
     * Este método garantiza que Glide siempre recibe una URL completa con http://...
     * @param url URL o ruta relativa de la foto.
     * @return URL absoluta lista para descargar, o null si no hay foto.
     */
    private String corregirUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http")) return url;
        // Si es ruta relativa, concatenamos la URL base del servidor
        String base = RetrofitClient.BASE_URL;
        if (base.endsWith("/") && url.startsWith("/")) {
            return base + url.substring(1);
        }
        return base + url;
    }

    /**
     * Devuelve el número total de pistas en la lista.
     * RecyclerView lo usa para saber cuántas filas debe crear.
     */
    @Override
    public int getItemCount() {
        return listaPistas != null ? listaPistas.size() : 0;
    }

    /**
     * ViewHolder: contenedor de referencias a los elementos visuales de una fila.
     *
     * En vez de llamar a findViewById() cada vez que se hace scroll
     * (lo cual es lento), se llama solo una vez en el constructor y
     * se guardan las referencias aquí para reutilizarlas.
     */
    static class PistaViewHolder extends RecyclerView.ViewHolder {
        /** ImageView donde se muestra la foto de la pista */
        ImageView ivFotoPista;
        /** TextView con el nombre de la pista */
        TextView tvNombrePista;
        /** TextView con el deporte de la pista */
        TextView tvDeporte;
        /** TextView con el estado de la pista (disponible/mantenimiento) */
        TextView tvEstado;

        /**
         * Construye el ViewHolder buscando las vistas en el XML item_pista.
         * @param itemView Vista raíz del item inflado desde item_pista.xml.
         */
        public PistaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFotoPista = itemView.findViewById(R.id.ivFotoPista);
            tvNombrePista = itemView.findViewById(R.id.tvNombrePista);
            tvDeporte = itemView.findViewById(R.id.tvDeporte);
            tvEstado = itemView.findViewById(R.id.tvEstado);
        }
    }
}
