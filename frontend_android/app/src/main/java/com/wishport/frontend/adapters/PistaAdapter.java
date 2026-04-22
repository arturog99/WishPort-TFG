package com.wishport.frontend.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wishport.frontend.R;
import com.wishport.frontend.models.Pista;

import java.util.List;

/**
 * Adapter para conectar la lista de Pistas con el RecyclerView.
 */
public class PistaAdapter extends RecyclerView.Adapter<PistaAdapter.PistaViewHolder> {

    private List<Pista> listaPistas;

    public interface OnItemClickListener {
        void onItemClick(Pista pista);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public PistaAdapter(List<Pista> listaPistas) {
        this.listaPistas = listaPistas;
    }

    @NonNull
    @Override
    public PistaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pista, parent, false);
        return new PistaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PistaViewHolder holder, int position) {
        Pista pista = listaPistas.get(position);
        holder.tvNombrePista.setText(pista.getNombre());
        holder.tvDeporte.setText(pista.getDeporte());
        holder.tvEstado.setText(pista.getEstado());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pista);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaPistas != null ? listaPistas.size() : 0;
    }

    /**
     * ViewHolder que contiene las vistas de cada item de pista.
     */
    static class PistaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombrePista, tvDeporte, tvEstado;

        public PistaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombrePista = itemView.findViewById(R.id.tvNombrePista);
            tvDeporte = itemView.findViewById(R.id.tvDeporte);
            tvEstado = itemView.findViewById(R.id.tvEstado);
        }
    }
}
