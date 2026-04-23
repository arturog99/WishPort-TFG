package com.wishport.frontend.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wishport.frontend.R;
import com.wishport.frontend.models.Reserva;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para conectar la lista de Reservas con el RecyclerView.
 */
public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder> {

    private List<Reserva> listaReservas;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    public interface OnItemClickListener {
        void onItemClick(Reserva reserva);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ReservaAdapter(List<Reserva> listaReservas) {
        this.listaReservas = listaReservas;
        this.dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reserva, parent, false);
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Reserva reserva = listaReservas.get(position);

        String deporte = reserva.getIdPista() != null ? reserva.getIdPista().getDeporte() : "Pádel";
        holder.tvDeporte.setText(deporte);

        String fecha = reserva.getFecha() != null ? dateFormat.format(reserva.getFecha()) : "24";
        String hora = reserva.getHoraInicio() != null ? timeFormat.format(reserva.getHoraInicio()) : "18:00";
        holder.tvInfoReserva.setText("Día " + fecha + " - " + hora + "h");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(reserva);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaReservas != null ? listaReservas.size() : 0;
    }

    /**
     * ViewHolder que contiene las vistas de cada item de reserva.
     */
    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeporte, tvInfoReserva;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeporte = itemView.findViewById(R.id.tvDeporte);
            tvInfoReserva = itemView.findViewById(R.id.tvInfoReserva);
        }
    }
}
