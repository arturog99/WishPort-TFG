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
 * ADAPTADOR DE PISTAS: Es el "traductor" que coge una lista de objetos Pista
 * y los dibuja uno a uno en el diseño (item_pista.xml) de la pantalla.
 */
public class PistaAdapter extends RecyclerView.Adapter<PistaAdapter.PistaViewHolder> {

    private List<Pista> listaPistas;
    private OnItemClickListener listener;

    /** Interfaz para avisar a la pantalla (Activity) cuando el usuario pulsa una pista */
    public interface OnItemClickListener {
        void onItemClick(Pista pista);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public PistaAdapter(List<Pista> listaPistas) {
        this.listaPistas = listaPistas;
    }

    /** Crea la "cáscara" visual de cada fila de la lista */
    @NonNull
    @Override
    public PistaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pista, parent, false);
        return new PistaViewHolder(view);
    }

    /** Rellena los huecos de la fila con los datos reales de una pista concreta */
    @Override
    public void onBindViewHolder(@NonNull PistaViewHolder holder, int position) {
        Pista pista = listaPistas.get(position);
        
        holder.tvNombrePista.setText(pista.getNombre());
        holder.tvDeporte.setText(pista.getDeporte());
        holder.tvEstado.setText("Estado: " + pista.getEstado());

        // GESTIÓN DE LA IMAGEN: Usamos la librería Glide para descargar y mostrar la foto
        String fotoUrl = corregirUrl(pista.getFotoUrl());

        Glide.with(holder.itemView.getContext())
                .load(fotoUrl)
                .placeholder(R.drawable.ic_pista_placeholder) // Imagen mientras carga
                .error(R.drawable.ic_pista_error)             // Imagen si falla la descarga
                .centerCrop()
                .into(holder.ivFotoPista);

        // Al pulsar en cualquier parte del item, avisamos al listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pista);
            }
        });
    }

    /** Asegura que la URL de la imagen sea completa para que Glide pueda descargarla */
    private String corregirUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http")) return url;
        
        // Si es una ruta relativa (ej: /img/pista1.jpg), le pegamos la base del servidor
        String base = RetrofitClient.BASE_URL;
        if (base.endsWith("/") && url.startsWith("/")) {
            return base + url.substring(1);
        }
        return base + url;
    }

    @Override
    public int getItemCount() {
        return listaPistas != null ? listaPistas.size() : 0;
    }

    /**
     * Clase interna que guarda las referencias a los Textos e Imágenes de cada fila
     * para no tener que buscarlos cada vez que el usuario hace scroll.
     */
    static class PistaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFotoPista;
        TextView tvNombrePista, tvDeporte, tvEstado;

        public PistaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFotoPista = itemView.findViewById(R.id.ivFotoPista);
            tvNombrePista = itemView.findViewById(R.id.tvNombrePista);
            tvDeporte = itemView.findViewById(R.id.tvDeporte);
            tvEstado = itemView.findViewById(R.id.tvEstado);
        }
    }
}
