package com.wishport.frontend.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wishport.frontend.R;
import com.wishport.frontend.models.Reserva;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ADAPTADOR DE RESERVAS: Transforma la lista de reservas del usuario en elementos visuales.
 * Se usa en la pantalla "Mis Reservas".
 */
public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder> {

    private List<Reserva> listaReservas;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private OnItemClickListener listener;

    /** Interfaz para detectar clics en una reserva */
    public interface OnItemClickListener {
        void onItemClick(Reserva reserva);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ReservaAdapter(List<Reserva> listaReservas) {
        this.listaReservas = listaReservas;
    }

    /** Permite actualizar la lista completa (ej: tras borrar una reserva) */
    public void actualizarLista(List<Reserva> nuevaLista) {
        this.listaReservas = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reserva, parent, false);
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Reserva reserva = listaReservas.get(position);

        // 1. Mostrar Deporte
        String deporte = (reserva.getIdPista() != null) ? reserva.getIdPista().getDeporte() : "Deporte";
        holder.tvDeporte.setText(deporte);

        // 2. Formatear Fecha y Hora (Ej: Día 15/05 - 18:00h)
        String fecha = (reserva.getFecha() != null) ? reserva.getFecha().format(dateFormatter) : "--/--";
        String hora = (reserva.getHoraInicio() != null) ? reserva.getHoraInicio().format(timeFormatter) : "--:--";
        holder.tvInfoReserva.setText("Día " + fecha + " - " + hora + "h");

        // 3. Click para ver el QR
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(reserva);
        });
    }

    @Override
    public int getItemCount() {
        return listaReservas != null ? listaReservas.size() : 0;
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeporte, tvInfoReserva;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeporte = itemView.findViewById(R.id.tvDeporte);
            tvInfoReserva = itemView.findViewById(R.id.tvInfoReserva);
        }
    }
}
